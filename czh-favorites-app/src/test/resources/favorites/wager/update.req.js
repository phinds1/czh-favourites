// Legacy front-end favorite-wager update body (gisorch wire contract).
// Parameters: groupNumber (slot id), gameName.
t = {
  name: "Updated Name",
  group: {
    id: groupNumber,
    name: "My Lotto Group"
  },
  wager: {
    type: "NORMAL",
    duration: 2,
    drawNames: ["WED", "FRI", "SUN"],
    gameName: gameName,
    price: 200,
    boards: [
      {
        systemBetType: "default",
        betType: "NORMAL",
        quickpick: false,
        primarySelections: ["7", "8", "9"]
      }
    ]
  }
};
