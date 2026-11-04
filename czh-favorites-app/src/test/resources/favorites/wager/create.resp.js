// Legacy wire shape (gisorch FavoriteWagerDTO) plus favorites-only additive attributes
// (playerId, gameName, flags, createdAt) which the front end ignores.
jsonDefinition = {
  id:          notNull(),
  name:        notNull(),
  group: {
    id:        notNull(),
    name:      notNull()
  },
  playerId:    notNull(),
  gameName:    notNull(),
  flags:       notNull(),
  createdAt:   notNull(),
  wager: {
    type:          "NORMAL",
    gameName:      notNull(),
    price:         notNull(),
    duration:      notNull(),
    drawNames:     notNull(),
    boards:        notNull()
  }
};
