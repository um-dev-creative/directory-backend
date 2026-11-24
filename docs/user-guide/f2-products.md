# Products: edit, deactivate, price and image (user guide)

Owners can keep their catalogue up to date: change a product, set a price and an image, hide it without losing it, and bring it back. Visitors only see active products.

All writes need the `session-token` header. Errors: `400` invalid data, `401` no session, `403` the product is not yours, `404` the product does not exist.

## Create a product
`POST /api/v1/products`

```json
{
  "name": "Sourdough loaf",
  "description": "Baked every morning",
  "categoryId": "<category uuid>",
  "price": 4.50,
  "currency": "CAD",
  "imageRef": "https://cdn.example.com/products/loaf.jpg",
  "businessId": "<your business uuid>"
}
```

- `price`, `currency` and `imageRef` are optional. A new product is **active**.
- **`businessId` (new, optional)**: when you send it, the product is created and linked to that business in one call (you must own the business: `404` if it does not exist, `403` if it is not yours). When you do not send it, the product is only created (you must own at least one business) and you link it later with `POST /api/v1/products/link`, exactly as before.

## Price
- `price` is zero or more, with at most 2 decimals (`4.5` and `4.50` are fine, `4.505` is not).
- `currency` is a 3-letter ISO code such as `USD`, `CAD`, `COP`. Lower case is accepted and stored in upper case.
- Send both or neither. A product without price is valid.

## Edit a product
- `PATCH /api/v1/products/{productId}` changes only the fields you send (`name`, `description`, `categoryId`, `price`, `currency`, `imageRef`, `active`). Sending only a new `price` keeps the stored currency. A blank `imageRef` removes the image.
- `PUT /api/v1/products/{productId}` replaces the product: `name`, `description` and `categoryId` are required; a price, currency or image you leave out is cleared.

Both answer `200` with the updated product.

## Deactivate and reactivate
- `DELETE /api/v1/products/{productId}` hides the product (`active = false`). Nothing is erased. It answers `204`, also when it was already inactive.
- `PATCH` with `{ "active": true }` shows it again.

## List the products of a business
`GET /api/v1/products/business/{businessId}?page=1&per_page=10`

- **Public**: only active products (the product and its link to the business must be active). Anyone can call it, with or without session.
- **Owner**: add `include_inactive=true` and your `session-token` to get all of them, and `active=false` or `active=true` to filter. Without a session you get `401`; if the business is not yours, `403`.

Each item has `id`, `name`, `description`, `active`, `price`, `currency`, `imageRef`, dates and the parent category.

## Who can edit a product
The user who created it, or the owner of any business that carries it.
