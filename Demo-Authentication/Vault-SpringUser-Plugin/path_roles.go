package secretsengine

import (
	"context"
	"fmt"
	"github.com/hashicorp/vault/sdk/framework"
	"github.com/hashicorp/vault/sdk/logical"
	"strings"
	"time"
)

// springBootRoleEntry defines the data required
// for ` Vault role to access and call the mongodb
type springBootRoleEntry struct {
	Name       string        `json:"name"`
	Database   string        `json:"database"`
	Collection string        `json:"collection"`
	Roles      []string      `json:"roles"`
	Class      string        `json:"class"`
	TTL        time.Duration `json:"ttl"`
	MaxTTL     time.Duration `json:"max_ttl"`
}

// toResponseData returns response data for a role
func (r *springBootRoleEntry) toResponseData() map[string]interface{} {
	respData := map[string]interface{}{
		"ttl":        r.TTL.Seconds(),
		"max_ttl":    r.MaxTTL.Seconds(),
		"username":   r.Database,
		"collection": r.Collection,
		"roles":      r.Roles,
		"class":      r.Class,
	}
	return respData
}

// pathRole extends the Vault API with a `/role`
// endpoint for the backend. You can choose whether
// or not certain attributes should be displayed,
// required, and named. You can also define different
// path patterns to list all Roles.
func pathRole(b *springBootUserBackend) []*framework.Path {
	return []*framework.Path{
		{
			Pattern: "role/" + framework.GenericNameRegex("name"),
			Fields: map[string]*framework.FieldSchema{
				"name": {
					Type:        framework.TypeLowerCaseString,
					Description: "Name of the role",
					Required:    true,
				},
				"database": {
					Type:        framework.TypeString,
					Description: "The database to use for the user account",
					Required:    true,
				},
				"collection": {
					Type:        framework.TypeString,
					Description: "The collection to use for the user account",
					Required:    true,
				},
				"roles": {
					Type:        framework.TypeString,
					Description: "Roles to add, comma separeted",
				},
				"class": {
					Type:        framework.TypeString,
					Description: "Class for Spring Boot",
					Required:    true,
				},
				"ttl": {
					Type:        framework.TypeDurationSecond,
					Description: "Default lease for generated credentials. If not set or set to 0, will use system default.",
				},
				"max_ttl": {
					Type:        framework.TypeDurationSecond,
					Description: "Maximum time for role. If not set or set to 0, will use system default.",
				},
			},
			Operations: map[logical.Operation]framework.OperationHandler{
				logical.ReadOperation: &framework.PathOperation{
					Callback: b.pathRolesRead,
				},
				logical.CreateOperation: &framework.PathOperation{
					Callback: b.pathRolesWrite,
				},
				logical.UpdateOperation: &framework.PathOperation{
					Callback: b.pathRolesWrite,
				},
				logical.DeleteOperation: &framework.PathOperation{
					Callback: b.pathRolesDelete,
				},
			},
			HelpSynopsis:    pathRoleHelpSynopsis,
			HelpDescription: pathRoleHelpDescription,
		},
		{
			Pattern: "role/?$",
			Operations: map[logical.Operation]framework.OperationHandler{
				logical.ListOperation: &framework.PathOperation{
					Callback: b.pathRolesList,
				},
			},
			HelpSynopsis:    pathRoleListHelpSynopsis,
			HelpDescription: pathRoleListHelpDescription,
		},
	}
}

// pathRolesList makes a request to Vault storage to retrieve a list of Roles for the backend
func (b *springBootUserBackend) pathRolesList(ctx context.Context, req *logical.Request, d *framework.FieldData) (*logical.Response, error) {
	entries, err := req.Storage.List(ctx, "role/")
	if err != nil {
		return nil, err
	}

	return logical.ListResponse(entries), nil
}

// pathRolesRead makes a request to Vault storage to read a role and return response data
func (b *springBootUserBackend) pathRolesRead(ctx context.Context, req *logical.Request, d *framework.FieldData) (*logical.Response, error) {
	entry, err := b.getRole(ctx, req.Storage, d.Get("name").(string))
	if err != nil {
		return nil, err
	}

	if entry == nil {
		return nil, nil
	}

	return &logical.Response{
		Data: entry.toResponseData(),
	}, nil
}

// pathRolesWrite makes a request to Vault storage to update a role based on the attributes passed to the role configuration
func (b *springBootUserBackend) pathRolesWrite(ctx context.Context, req *logical.Request, d *framework.FieldData) (*logical.Response, error) {
	name, ok := d.GetOk("name")
	if !ok {
		return logical.ErrorResponse("missing role name"), nil
	}

	roleEntry, err := b.getRole(ctx, req.Storage, name.(string))
	if err != nil {
		return nil, err
	}

	if roleEntry == nil {
		roleEntry = &springBootRoleEntry{}
	}

	roleEntry.Name = name.(string)

	createOperation := req.Operation == logical.CreateOperation

	if database, ok := d.GetOk("database"); ok {
		roleEntry.Database = database.(string)
	} else if !ok && createOperation {
		return nil, fmt.Errorf("missing database in role")
	}

	if collection, ok := d.GetOk("collection"); ok {
		roleEntry.Collection = collection.(string)
	} else if !ok && createOperation {
		return nil, fmt.Errorf("missing collection in role")
	}

	if roles, ok := d.GetOk("roles"); ok {
		roleEntry.Roles = strings.Split(roles.(string), ",")
	} else if !ok && createOperation {
		return nil, fmt.Errorf("missing Roles in role")
	}

	if class, ok := d.GetOk("class"); ok {
		roleEntry.Class = class.(string)
	} else if !ok && createOperation {
		return nil, fmt.Errorf("missing Class in role")
	}

	if ttlRaw, ok := d.GetOk("ttl"); ok {
		roleEntry.TTL = time.Duration(ttlRaw.(int)) * time.Second
	} else if createOperation {
		roleEntry.TTL = time.Duration(d.Get("ttl").(int)) * time.Second
	}

	if maxTTLRaw, ok := d.GetOk("max_ttl"); ok {
		roleEntry.MaxTTL = time.Duration(maxTTLRaw.(int)) * time.Second
	} else if createOperation {
		roleEntry.MaxTTL = time.Duration(d.Get("max_ttl").(int)) * time.Second
	}

	if roleEntry.MaxTTL != 0 && roleEntry.TTL > roleEntry.MaxTTL {
		return logical.ErrorResponse("ttl cannot be greater than max_ttl"), nil
	}

	if err := setRole(ctx, req.Storage, name.(string), roleEntry); err != nil {
		return nil, err
	}

	return nil, nil
}

// pathRolesDelete makes a request to Vault storage to delete a role
func (b *springBootUserBackend) pathRolesDelete(ctx context.Context, req *logical.Request, d *framework.FieldData) (*logical.Response, error) {
	name := d.Get("Name").(string)
	err := req.Storage.Delete(ctx, "role/"+name)
	if err != nil {
		return nil, fmt.Errorf("error deleting the user account: %w", err)
	}

	return nil, nil
}

// setRole adds the role to the Vault storage API
func setRole(ctx context.Context, s logical.Storage, name string, roleEntry *springBootRoleEntry) error {
	entry, err := logical.StorageEntryJSON("role/"+name, roleEntry)
	if err != nil {
		return err
	}

	if entry == nil {
		return fmt.Errorf("failed to create storage entry for role")
	}

	if err := s.Put(ctx, entry); err != nil {
		return err
	}

	return nil
}

// getRole gets the role from the Vault storage API
func (b *springBootUserBackend) getRole(ctx context.Context, s logical.Storage, name string) (*springBootRoleEntry, error) {
	if name == "" {
		return nil, fmt.Errorf("missing role name")
	}

	entry, err := s.Get(ctx, "role/"+name)
	if err != nil {
		return nil, err
	}

	if entry == nil {
		return nil, nil
	}

	var role springBootRoleEntry

	if err := entry.DecodeJSON(&role); err != nil {
		return nil, err
	}
	return &role, nil
}

const (
	pathRoleHelpSynopsis    = `Manages the Vault role for generating user accounts in a mongodb.`
	pathRoleHelpDescription = `
	This path allows you to read and write Roles used to generate a user account.	
	`

	pathRoleListHelpSynopsis    = `List the existing Roles in springBootUser backend`
	pathRoleListHelpDescription = `Roles will be listed by the role name.`
)
