// Legacy wire shape (gisorch FavoriteWagerGroupDTO): id is the slot number as a string.
jsonDefinition = {
  id:          notNull(),
  name:        notNull(),
  playerId:    notNull(),
  flags:       notNull(),
  createdAt:   notNull(),
  updatedAt:   notNull()
};
