const path = require('path');
const fs = require('fs-extra');
const { readdir, stat } = require('fs/promises');
const yaml = require('js-yaml');

// HardHat Plugins
require('solidity-coverage');
require('@nomiclabs/hardhat-truffle5');
require('@nomicfoundation/hardhat-network-helpers');
require('@nomiclabs/hardhat-web3');
require('@nomiclabs/hardhat-ethers');

// HardHat Tasks
task('compile', 'Compiles the entire project, building all artifacts and deletes build-info if needed')
	.addOptionalParam('preserveBuildInfo', 'Do not delete build-info even if compile size exceeds the configured limit', false, types.boolean)
	.setAction(async (taskArgs, hre, runSuper) => {
		await runSuper();
		// Post-hook
		const buildInfoPath = path.join(hre.config.paths.artifacts, 'build-info');
		const size = await dirSize(buildInfoPath);
		if (!taskArgs.preserveBuildInfo && size > hre.config.adhara.buildInfoLimit) {
			console.log(`Deleting ${buildInfoPath} (${(size / 1000 / 1000).toFixed(2)}M)...`);
			fs.removeSync(buildInfoPath);
		}
	});

task('specific-compile', 'Compiles specific sources, building artifacts')
	.addParam('sources', 'Path to sources')
	.setAction(async (taskArgs) => {
		hre.config.paths.sources = path.resolve(process.cwd(), taskArgs.sources);
		await hre.run('compile');
	});

task('specific-test', 'Runs mocha tests compiling only for specific sources and test files')
	.addParam('sources', 'Path to sources')
	.addParam('tests', 'Path to tests')
	.addFlag('bail', 'Stop running tests after the first test failure')
	.addFlag('parallel', 'Run tests in parallel')
	.setAction(async (taskArgs) => {
		hre.config.paths.sources = path.resolve(process.cwd(), taskArgs.sources);
		hre.config.paths.tests = path.resolve(process.cwd(), taskArgs.tests);
		await hre.run('test', { bail: taskArgs.bail, parallel: taskArgs.parallel });
	});

task('specific-coverage', 'Generates a code coverage report for specific sources and test files')
	.addParam('sources', 'Path to sources')
	.addParam('tests', 'Path to tests')
	.setAction(async (taskArgs) => {
		hre.config.paths.sources = path.resolve(process.cwd(), taskArgs.sources);
		hre.config.paths.tests = path.resolve(process.cwd(), taskArgs.tests);
		await hre.run('coverage', { testfiles: hre.config.paths.tests });
	});

// HardHat Configuration
// extendEnvironment((hre) => {
// 	const Web3 = require("web3");
// 	hre.Web3 = Web3;
// 	// hre.network.provider is an EIP1193-compatible provider.
// 	hre.web3 = new Web3(hre.network.provider);
// });

/** @type import('hardhat/config').HardhatUserConfig */
module.exports = {
	runWithSnapshotting: true,
	networks: {
		hardhat: {
			allowUnlimitedContractSize: true,
			blockGasLimit: 200000000,
			initialBaseFeePerGas: 0,
			accounts: {
				count: 60
			}
		},
	},
	solidity: {
		version: '0.8.17',
		settings: {
			optimizer: {
				enabled: true, //(!process.env.DEBUG),
				runs: 1000,
				details: {
					yul: true,
				},
			},
			viaIR: true,
			evmVersion: 'petersburg',
		},
	},
	paths: {
		sources: './contracts',
		tests: './test',
		cache: './cache',
		artifacts: './build',
	},
	mocha: {
		timeout: 600000,
	},
	adhara: {
		buildInfoLimit: 85000000, // Bytes - Adjust to lower value if still getting OOM
	},
};

/// /////////////////////////////////////////////////////////////////////////////

// Helper functions
const dirSize = async (directory) => {
	try {
		const files = await readdir(directory);
		const stats = files.map((file) => stat(path.join(directory, file)));
		return (await Promise.all(stats)).reduce((accumulator, { size }) => accumulator + size, 0);
	} catch (e) {
		return 0;
	}
};
