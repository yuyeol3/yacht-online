export const SCORE_CATEGORIES = [
  { key: "ones", label: "Ones", upper: "ONES" },
  { key: "twos", label: "Twos", upper: "TWOS" },
  { key: "threes", label: "Threes", upper: "THREES" },
  { key: "fours", label: "Fours", upper: "FOURS" },
  { key: "fives", label: "Fives", upper: "FIVES" },
  { key: "sixes", label: "Sixes", upper: "SIXES" },
  { key: "choice", label: "Choice", upper: "CHOICE" },
  { key: "fourOfAKind", label: "4 of a Kind", upper: "FOUR_OF_A_KIND" },
  { key: "fullHouse", label: "Full House", upper: "FULL_HOUSE" },
  { key: "sStraight", label: "S. Straight", upper: "S_STRAIGHT" },
  { key: "lStraight", label: "L. Straight", upper: "L_STRAIGHT" },
  { key: "yacht", label: "Yacht", upper: "YACHT" }
];

export function calcPossibleScores(dice) {
  if (!dice || dice.length !== 5) return null;
  const scoreTable = [0, 0, 0, 0, 0, 0, 0];
  for (const val of dice) {
    scoreTable[val]++;
  }

  const result = {
    ones: scoreTable[1] * 1,
    twos: scoreTable[2] * 2,
    threes: scoreTable[3] * 3,
    fours: scoreTable[4] * 4,
    fives: scoreTable[5] * 5,
    sixes: scoreTable[6] * 6,
    choice: 0,
    fourOfAKind: null,
    fullHouse: null,
    sStraight: null,
    lStraight: null,
    yacht: null
  };

  for (let i = 1; i <= 6; i++) {
    result.choice += scoreTable[i] * i;
  }

  for (let i = 1; i <= 6; i++) {
    if (scoreTable[i] >= 4) {
      result.fourOfAKind = result.choice;
      break;
    }
  }

  for (let i = 1; i <= 6; i++) {
    for (let j = i + 1; j <= 6; j++) {
      if ((scoreTable[i] === 2 && scoreTable[j] === 3) || (scoreTable[i] === 3 && scoreTable[j] === 2)) {
        result.fullHouse = result.choice;
      }
    }
  }

  if (
    (scoreTable[1] && scoreTable[2] && scoreTable[3] && scoreTable[4]) ||
    (scoreTable[2] && scoreTable[3] && scoreTable[4] && scoreTable[5]) ||
    (scoreTable[3] && scoreTable[4] && scoreTable[5] && scoreTable[6])
  ) {
    result.sStraight = 15;
  }

  if (
    (scoreTable[1] && scoreTable[2] && scoreTable[3] && scoreTable[4] && scoreTable[5]) ||
    (scoreTable[2] && scoreTable[3] && scoreTable[4] && scoreTable[5] && scoreTable[6])
  ) {
    result.lStraight = 30;
  }

  for (let i = 1; i <= 6; i++) {
    if (scoreTable[i] === 5) {
      result.yacht = 50;
    }
  }

  return result;
}

export function upperKeyToLower(upper) {
  return SCORE_CATEGORIES.find((c) => c.upper === upper)?.key ?? null;
}
