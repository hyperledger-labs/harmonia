/**
 * Use this file to configure your truffle project. It's seeded with some
 * common settings for different networks and features like migrations,
 * compilation and testing. Uncomment the ones you need or modify
 * them to suit your project as necessary.
 *
 * More information about configuration can be found at:
 *
 * trufflesuite.com/docs/advanced/configuration
 *
 * To deploy via Infura you'll need a wallet provider (like @truffle/hdwallet-provider)
 * to sign your transactions before they're sent to a remote public node. Infura accounts
 * are available for free at: infura.io/register.
 *
 * You'll also need a mnemonic - the twelve word phrase the wallet uses to generate
 * public/private key pairs. If you're publishing your code to GitHub make sure you load this
 * phrase from a file you've .gitignored so it doesn't accidentally become public.
 *
 */

const HDWalletProvider = require('@truffle/hdwallet-provider');

module.exports = {
  /**
   * Networks define how you connect to your ethereum client and let you set the
   * defaults web3 uses to send transactions. If you don't specify one truffle
   * will spin up a development blockchain for you on port 9545 when you
   * run `develop` or `test`. You can ask a truffle command to use a specific
   * network from the command line, e.g
   *
   * $ truffle test --network <network-name>
   */

  networks: {
    // Useful for testing. The `development` name is special - truffle uses it by default
    // if it's defined here and no other network is specified at the command line.
    // You should run a client (like ganache-cli, geth or parity) in a separate terminal
    // tab if you use this network and you must also set the `host`, `port` and `network_id`
    // options below to some value.
    //
    development: {
      host: '127.0.0.1',     // Localhost (default: none)
      port: 8545,            // Standard Ethereum port (default: none)
      network_id: '*',       // Any network (default: none)
    },
    besu_harmonia: {
      host: '127.0.0.1',     // Localhost (default: none)
      port: 8545,            // Standard Ethereum port (default: none)
      network_id: '44844',   // Any network (default: none)
      gas: 100000000,
      gasPrice: 0,
      from: '627306090abaB3A6e1400e9345bC60c78a8BEf57',
      provider: function () {
        return new HDWalletProvider({
          providerOrUrl: 'http://127.0.0.1:8545',
          privateKeys: ['3f841bf589fdf83a521e55d51afddc34fa65351161eead24f064855fc29c9580', '9549f39decea7b7504e15572b2c6a72766df0281cea22bd1a3bc87166b1ca290', 'c87509a1c067bbde78beb793e6fa76530b6382a4c0241e5e4a9ec0a0f44dc0d3']
        })
      },
    },
  },
  rpc: {
    host: '127.0.0.1',
    port: 8545
  },

  // Set default mocha options here, use special reporters etc.
  mocha: {
    reporter: 'mocha-truffle-reporter',
    reporterOptions: {
      excludeContracts: ['Migrations']
    },
    //timeout: 1200000
  },

  // Configure your compilers
  compilers: {
    solc: {
      version: '0.8.13',    // Fetch exact version from solc-bin (default: truffle's version)
      // docker: true,        // Use '0.5.1' you've installed locally with docker (default: false)
      // settings: {          // See the solidity docs for advice about optimization and evmVersion
      //  optimizer: {
      //    enabled: false,
      //    runs: 200
      //  },
      //  evmVersion: 'byzantium'
      // }
    }
  },
};
