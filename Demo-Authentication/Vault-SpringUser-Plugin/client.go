package secretsengine

import (
	"context"
	"errors"
	"go.mongodb.org/mongo-driver/mongo"
	mongoOptions "go.mongodb.org/mongo-driver/mongo/options"
)

// springBootUserClient creates an object storing
// the client.
type springBootUserClient struct {
	client *mongo.Client
}

// newClient creates a new client to access database
// and create user entries.
func newClient(config *springBootUserConfig) (*springBootUserClient, error) {
	if config == nil {
		return nil, errors.New("client configuration was nil")
	}

	if config.Username == "" {
		return nil, errors.New("client username was not defined")
	}

	if config.Password == "" {
		return nil, errors.New("client Password was not defined")
	}

	if config.URL == "" {
		return nil, errors.New("client URL was not defined")
	}

	credential := mongoOptions.Credential{
		AuthSource: "admin",
		Username:   config.Username,
		Password:   config.Password,
	}

	clientOptions := mongoOptions.Client().ApplyURI(config.URL).SetAuth(credential)

	c, err := mongo.Connect(context.TODO(), clientOptions)
	if err != nil {
		return nil, err
	}
	return &springBootUserClient{c}, nil
}
