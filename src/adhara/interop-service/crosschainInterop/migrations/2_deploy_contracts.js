
async function doDeploy(deployer, network, accounts) {
  for (let i = 0; i < accounts.length; i++) {
    await web3.eth.sendTransaction({
      from: '627306090abaB3A6e1400e9345bC60c78a8BEf57',
      to: accounts[i],
      value: 1000000000000000
    })
  }
}

module.exports = (deployer, network, accounts) => {
  deployer.then(async () => {
    await doDeploy(deployer, network, accounts);
  });
};
