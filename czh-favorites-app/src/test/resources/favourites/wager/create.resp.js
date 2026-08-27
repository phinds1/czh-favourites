jsonDefinition = {
  id:          notNull(),
  playerId:    notNull(),
  groupNumber: notNull(),
  gameName:    notNull(),
  wagerName:   notNull(),
  flags:       0,
  createdAt:   notNull(),
  wager: {
    gameName:     notNull(),
    stake:        100,
    price:        100,
    duration:     1,
    serialNumber: notNull(),
    boards:       notNull()
  }
};
