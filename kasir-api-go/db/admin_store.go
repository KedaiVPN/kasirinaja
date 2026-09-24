package db

import (
	"context"

	"github.com/jackc/pgx/v5/pgtype"
)

type AdminStoreListItem struct {
	ID        pgtype.UUID `json:"id"`
	StoreName string      `json:"store_name"`
	OwnerName string      `json:"owner_name"`
	IsBlocked bool        `json:"is_blocked"`
}

type AdminStoreDetail struct {
	ID           pgtype.UUID        `json:"id"`
	StoreName    string             `json:"store_name"`
	OwnerName    string             `json:"owner_name"`
	Email        string             `json:"email"`
	Phone        string             `json:"phone"`
	CashierCount int64              `json:"cashier_count"`
	IsPro        bool               `json:"is_pro"`
	ProExpiresAt pgtype.Timestamptz `json:"pro_expires_at"`
	IsBlocked    bool               `json:"is_blocked"`
}

const countTotalStores = `SELECT COUNT(*) FROM stores`

func (q *Queries) CountTotalStores(ctx context.Context) (int64, error) {
	row := q.db.QueryRow(ctx, countTotalStores)
	var count int64
	err := row.Scan(&count)
	return count, err
}

const countProStores = `SELECT COUNT(*) FROM stores WHERE pro_expires_at IS NOT NULL AND pro_expires_at > CURRENT_TIMESTAMP`

func (q *Queries) CountProStores(ctx context.Context) (int64, error) {
	row := q.db.QueryRow(ctx, countProStores)
	var count int64
	err := row.Scan(&count)
	return count, err
}

const countNonProStores = `SELECT COUNT(*) FROM stores WHERE pro_expires_at IS NULL OR pro_expires_at <= CURRENT_TIMESTAMP`

func (q *Queries) CountNonProStores(ctx context.Context) (int64, error) {
	row := q.db.QueryRow(ctx, countNonProStores)
	var count int64
	err := row.Scan(&count)
	return count, err
}

const listAdminStores = `
SELECT
	s.id,
	s.store_name,
	COALESCE(u.full_name, '') AS owner_name,
	COALESCE(s.is_blocked, false) AS is_blocked
FROM stores s
LEFT JOIN users u ON u.id = s.owner_id
ORDER BY s.created_at DESC
`

func (q *Queries) ListAdminStores(ctx context.Context) ([]AdminStoreListItem, error) {
	rows, err := q.db.Query(ctx, listAdminStores)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var items []AdminStoreListItem
	for rows.Next() {
		var i AdminStoreListItem
		if err := rows.Scan(&i.ID, &i.StoreName, &i.OwnerName, &i.IsBlocked); err != nil {
			return nil, err
		}
		items = append(items, i)
	}
	if err := rows.Err(); err != nil {
		return nil, err
	}
	return items, nil
}

const getAdminStoreDetail = `
SELECT
	s.id,
	s.store_name,
	COALESCE(u.full_name, '') AS owner_name,
	COALESCE(u.email, '') AS email,
	COALESCE(s.phone, u.phone, '') AS phone,
	(SELECT COUNT(*) FROM users WHERE store_id = s.id AND role = 'kasir') AS cashier_count,
	(s.pro_expires_at IS NOT NULL AND s.pro_expires_at > CURRENT_TIMESTAMP) AS is_pro,
	s.pro_expires_at,
	COALESCE(s.is_blocked, false) AS is_blocked
FROM stores s
LEFT JOIN users u ON u.id = s.owner_id
WHERE s.id = $1
`

func (q *Queries) GetAdminStoreDetail(ctx context.Context, id pgtype.UUID) (AdminStoreDetail, error) {
	row := q.db.QueryRow(ctx, getAdminStoreDetail, id)
	var i AdminStoreDetail
	err := row.Scan(
		&i.ID,
		&i.StoreName,
		&i.OwnerName,
		&i.Email,
		&i.Phone,
		&i.CashierCount,
		&i.IsPro,
		&i.ProExpiresAt,
		&i.IsBlocked,
	)
	return i, err
}
