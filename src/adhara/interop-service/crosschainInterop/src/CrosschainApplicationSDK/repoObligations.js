function init(dependencies) {

  async function createRepoObligation(networkId, body) {

    const openingLeg = {
      tradeId: body.tradeId,
      fromAccount: body.openingLeg.fromAccount,
      toAccount: body.openingLeg.toAccount,
      amount: body.openingLeg.amount
    }

    const closingLeg = {
      tradeId: body.tradeId,
      fromAccount: body.closingLeg.fromAccount,
      toAccount: body.closingLeg.toAccount,
      amount: body.closingLeg.amount,
      timestamp: body.closingLeg.timestamp
    }

    // Don't wait for the closing leg
    dependencies.createSettlementObligation(networkId, closingLeg)
    return await dependencies.createSettlementObligation(networkId, openingLeg)
  }

  return {
    createRepoObligation
  }
}

module.exports = init
