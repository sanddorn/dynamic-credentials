package secretsengine

import (
	"context"
	"errors"
	"fmt"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/mongo"
	"math/rand"
	"time"

	"github.com/hashicorp/vault/sdk/framework"
	"github.com/hashicorp/vault/sdk/logical"
)

const (
	springBootUserTokenType = "spring_user_token"
	usernamePrefix          = "usr"
	charsetWithNumbers      = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
	charsetCharacter        = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
	charsetFull             = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!$%&/()=?@"
)

// springUserToken defines a secret for the
type springUserToken struct {
	Username string `json:"username"`
	Password string `json:"password"`
	Token    string `json:"token"`
}

type SpringUser struct {
	Id       string   `bson:"_id,omitempty"`
	Password string   `bson:"password,omitempty"`
	Roles    []string `bson:"roles,omitempty"`
	Class    string   `bson:"_class,omitempty"`
}

var seededRand *rand.Rand

// springUserToken defines a secret to store for a given role
// and how it should be revoked or renewed.
func (b *springBootUserBackend) springBootUserToken() *framework.Secret {
	seededRand = rand.New(
		rand.NewSource(time.Now().UnixNano()))
	return &framework.Secret{
		Type: springBootUserTokenType,
		Fields: map[string]*framework.FieldSchema{
			"token": {
				Type:        framework.TypeString,
				Description: "Spring Boot User Token",
			},
		},
		Revoke: b.tokenRevoke,
		Renew:  b.tokenRenew,
	}
}

// tokenRevoke removes the token from the Vault storage API and calls the client to revoke the token
func (b *springBootUserBackend) tokenRevoke(ctx context.Context, req *logical.Request, d *framework.FieldData) (*logical.Response, error) {
	client, err := b.getClient(ctx, req.Storage)
	if err != nil {
		return nil, fmt.Errorf("error getting client: %w", err)
	}

	token := ""
	// We passed the token using InternalData from when we first created
	// the secret. From a security standpoint, your target API and client
	// should use a token ID instead!
	tokenRaw, ok := req.Secret.InternalData["token"]
	if ok {
		token, ok = tokenRaw.(string)
		if !ok {
			return nil, fmt.Errorf("invalid value for token in secret internal data")
		}
	}
	database := ""
	databaseRaw, ok := req.Secret.InternalData["database"]
	if ok {
		database, ok = databaseRaw.(string)
		if !ok {
			return nil, fmt.Errorf("invalid value for database in secret internal data")
		}
	}

	collection := ""
	collectionRaw, ok := req.Secret.InternalData["collection"]
	if ok {
		collection, ok = collectionRaw.(string)
		if !ok {
			return nil, fmt.Errorf("invalid value for collection in secret internal data")
		}
	}

	collectionConnection := client.client.Database(database).Collection(collection)

	if err := deleteToken(ctx, collectionConnection, token); err != nil {
		return nil, fmt.Errorf("error revoking user token: %w", err)
	}
	return nil, nil
}

// tokenRenew calls the client to create a new token and stores it in the Vault storage API
func (b *springBootUserBackend) tokenRenew(ctx context.Context, req *logical.Request, d *framework.FieldData) (*logical.Response, error) {
	roleRaw, ok := req.Secret.InternalData["role"]
	if !ok {
		return nil, fmt.Errorf("secret is missing role internal data")
	}

	// get the role entry
	role := roleRaw.(string)
	roleEntry, err := b.getRole(ctx, req.Storage, role)
	if err != nil {
		return nil, fmt.Errorf("error retrieving role: %w", err)
	}

	if roleEntry == nil {
		return nil, errors.New("error retrieving role: role is nil")
	}

	resp := &logical.Response{Secret: req.Secret}

	if roleEntry.TTL > 0 {
		resp.Secret.TTL = roleEntry.TTL
	}
	if roleEntry.MaxTTL > 0 {
		resp.Secret.MaxTTL = roleEntry.MaxTTL
	}

	return resp, nil
}

// createToken calls the mongodb client to sign in and returns a new SpringUserToken containing username and password
func createToken(ctx context.Context, c *springBootUserClient, collection *mongo.Collection, roles []string, class string) (*springUserToken, error) {
	username := usernamePrefix + randomString(6, charsetCharacter) + randomString(7, charsetWithNumbers)
	password := randomString(16, charsetFull)
	userDocument :=
		SpringUser{
			Id:       username,
			Password: password,
			Roles:    roles,
			Class:    class,
		}
	_, err := collection.InsertOne(context.TODO(), userDocument)
	if err != nil {
		return nil, err
	}

	return &springUserToken{
		Password: password,
		Token:    username,
		Username: username,
	}, nil
}

// deleteToken calls the mongodb client to remove the user from the database.
func deleteToken(ctx context.Context, collection *mongo.Collection, token string) error {

	//userFilter := bson.M{"_id": token}
	result, err := collection.DeleteOne(context.TODO(), bson.M{"_id": token})
	if err != nil {
		return errors.New("cannot delete user '" + token + "'" + err.Error())
	}

	if result.DeletedCount == 0 {
		return errors.New("User '" + token + "' not found.")
	}

	return nil
}

func randomString(length int, charset string) string {
	b := make([]byte, length)
	for i := range b {
		b[i] = charset[seededRand.Intn(len(charset))]
	}
	return string(b)
}
