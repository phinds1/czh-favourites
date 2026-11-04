// Legacy wire shape (gisorch FavoriteWagerDTO) via the admin read path.
jsonDefinition = {
  id:          notNull(),
  name:        notNull(),
  group:       notNull(),
  playerId:    notNull(),
  gameName:    notNull(),
  flags:       notNull(),
  createdAt:   notNull(),
  wager:       notNull()
};
