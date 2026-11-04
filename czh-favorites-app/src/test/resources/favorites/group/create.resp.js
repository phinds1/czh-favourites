// Legacy wire shape (gisorch FavoriteWagerGroupDTO): id is the slot number as a string;
// playerId/flags/createdAt/updatedAt are favorites-only additive attributes.
jsonDefinition = {
  id:          notNull(),
  name:        notNull(),
  playerId:    notNull(),
  flags:       notNull(),
  createdAt:   notNull(),
  updatedAt:   notNull()
};
