package secretsengine

import (
	"context"
	"errors"
	"fmt"

	"github.com/hashicorp/vault/sdk/framework"
	"github.com/hashicorp/vault/sdk/logical"
)

// pathCredentials extends the Vault API with a `/creds`
// endpoint for a role. You can choose whether
// or not certain attributes should be displayed,
// required, and named.
func pathCredentials(b *springBootUserBackend) *framework.Path {
	return &framework.Path{
		Pattern: "creds/" + framework.GenericNameRegex("name"),
		Fields: map[string]*framework.FieldSchema{
			"name": {
				Type:        framework.TypeLowerCaseString,
				Description: "Name of the role",
				Required:    true,
			},
		},
		Callbacks: map[logical.Operation]framework.OperationFunc{
			logical.ReadOperation:   b.pathCredentialsRead,
			logical.UpdateOperation: b.pathCredentialsRead,
		},
		HelpSynopsis:    pathCredentialsHelpSyn,
		HelpDescription: pathCredentialsHelpDesc,
	}
}

// pathCredentialsRead creates a new User-Account each time it is called if a
// role exists.
func (b *springBootUserBackend) pathCredentialsRead(ctx context.Context, req *logical.Request, d *framework.FieldData) (*logical.Response, error) {
	roleName := d.Get("name").(string)

	roleEntry, err := b.getRole(ctx, req.Storage, roleName)
	if err != nil {
		return nil, fmt.Errorf("error retrieving role: %w", err)
	}

	if roleEntry == nil {
		return nil, errors.New("error retrieving role: role is nil")
	}

	return b.createUserCreds(ctx, req, roleEntry)
}

// createUserCreds creates a new User Account token to store into the Vault backend, generates
// a response with the secrets information, and checks the TTL and MaxTTL attributes.
func (b *springBootUserBackend) createUserCreds(ctx context.Context, req *logical.Request, role *springBootRoleEntry) (*logical.Response, error) {
	token, err := b.createToken(ctx, req.Storage, role)
	if err != nil {
		return nil, err
	}

	// The response is divided into two objects (1) internal data and (2) data.
	// If you want to reference any information in your code, you need to
	// store it in internal data!
	resp := b.Secret(springBootUserTokenType).Response(map[string]interface{}{
		"token":    token.Token,
		"username": token.Username,
		"password": token.Password,
	}, map[string]interface{}{
		"token":      token.Token,
		"role":       role.Name,
		"database":   role.Database,
		"collection": role.Collection,
	})

	if role.TTL > 0 {
		resp.Secret.TTL = role.TTL
	}

	if role.MaxTTL > 0 {
		resp.Secret.MaxTTL = role.MaxTTL
	}

	return resp, nil
}

// createToken uses the mongodb client to generate a new user account.
func (b *springBootUserBackend) createToken(ctx context.Context, s logical.Storage, roleEntry *springBootRoleEntry) (*springUserToken, error) {
	client, err := b.getClient(ctx, s)
	if err != nil {
		return nil, err
	}

	var token *springUserToken

	// Create Collection from DB-Client
	collection := client.client.Database(roleEntry.Database).Collection(roleEntry.Collection)

	token, err = createToken(ctx, client, collection, roleEntry.Roles, roleEntry.Class)
	if err != nil {
		return nil, fmt.Errorf("error creating spring boot user account: %w", err)
	}

	if token == nil {
		return nil, errors.New("error creating the spring boot user account")
	}

	return token, nil
}

const pathCredentialsHelpSyn = `
Generate a user account from a specific Vault role.
`

const pathCredentialsHelpDesc = `
This path generates a spring boot user 
based on a particular role.
`
