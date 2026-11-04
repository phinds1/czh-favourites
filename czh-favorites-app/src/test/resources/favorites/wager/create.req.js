// Legacy front-end favorite-wager create body (gisorch wire contract).
// Parameters: groupNumber (slot id), gameName.
// Mirrors references/gis .../favorites/sportka.json structure.
t = {
  name: "My Lotto Ticket",
  group: {
    id: groupNumber,
    name: "My Lotto Group"
  },
  wager: {
    type: "NORMAL",
    duration: 1,
    drawNames: ["WED", "FRI", "SUN"],
    gameName: gameName,
    price: 100,
    boards: [
      {
        systemBetType: "default",
        betType: "NORMAL",
        quickpick: false,
        primarySelections: ["1", "2", "3", "4", "5", "6"]
      }
    ]
  }
};
