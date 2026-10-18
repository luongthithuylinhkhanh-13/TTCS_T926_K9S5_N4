
const toAmount = (value) => {
  const amount = Number(value);
  return Number.isFinite(amount) ? amount : 0;
};

const roundMoney = (value) =>
  Math.round((value + Number.EPSILON) * 100) / 100;

export const calculateVariance = (budget, actual) => {
  const budgetAmount = roundMoney(toAmount(budget));
  const actualAmount = roundMoney(toAmount(actual));
  const difference = roundMoney(actualAmount - budgetAmount);

  return {
    budget: budgetAmount,
    actual: actualAmount,
    difference,
    percentage:
      budgetAmount === 0
        ? null
        : roundMoney((difference / budgetAmount) * 100),
    status:
      difference > 0
        ? 'OVER_BUDGET'
        : difference < 0
          ? 'UNDER_BUDGET'
          : 'ON_BUDGET',
  };
};

export const calculateActualCostVariance = (
  budgetTree = [],
  actualCosts = [],
) => {
  const costsByWbsId = new Map();

  for (const cost of Array.isArray(actualCosts) ? actualCosts : []) {
    if (cost.wbsItemId == null) continue;

    const id = String(cost.wbsItemId);
    costsByWbsId.set(
      id,
      (costsByWbsId.get(id) || 0) + toAmount(cost.amount),
    );
  }

  const processNode = (node) => {
    const children = Array.isArray(node.children)
      ? node.children.map(processNode)
      : [];

    const directActual = costsByWbsId.get(String(node.id)) || 0;
    const childActual = children.reduce(
      (sum, child) => sum + child.actual,
      0,
    );

    const actual = roundMoney(directActual + childActual);
    const budget = toAmount(node.totalPrice);
    const variance = calculateVariance(budget, actual);

    return {
      ...node,
      children,
      directActual: roundMoney(directActual),
      budget: variance.budget,
      actual,
      difference: variance.difference,
      percentage: variance.percentage,
      status: variance.status,
    };
  };

  const items = (Array.isArray(budgetTree) ? budgetTree : [])
    .map(processNode);

  const totalBudget = roundMoney(
    items.reduce((sum, item) => sum + item.budget, 0),
  );

  const totalActual = roundMoney(
    (Array.isArray(actualCosts) ? actualCosts : [])
      .reduce((sum, cost) => sum + toAmount(cost.amount), 0),
  );

  return {
    items,
    ...calculateVariance(totalBudget, totalActual),
  };
};
