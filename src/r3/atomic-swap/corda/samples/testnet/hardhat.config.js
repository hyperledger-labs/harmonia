require("@nomiclabs/hardhat-waffle");

task("accounts", "Prints the list of accounts", async (taskArgs, hre) => {
  const accounts = await hre.ethers.getSigners();

  for (const account of accounts) {
    console.log(account.address);
  }
});

/**
 * @type import('hardhat/config').HardhatUserConfig
 */
module.exports = {
  solidity: "0.8.16",
  optimizer: {enabled: true},
  networks: {
    hardhat: {
      mining: {
        mempool: {
          order: "fifo",
        },
        auto: false,
        interval: 1000,
      },
      chainId: 1337,
    },
  },
};
