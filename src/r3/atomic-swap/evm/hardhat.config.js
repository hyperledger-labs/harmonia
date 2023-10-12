require("@nomiclabs/hardhat-waffle");
require("@nomicfoundation/hardhat-foundry");

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
  solidity: "0.8.20",
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
  paths: {
    sources: "./src",
    tests: "./test",
    cache: "./build/cache",
    artifacts: "./build/out"
  },
  foundry: {
    remappings: "./remappings.txt",
  },
};
