const { BigNumber } = require("ethers");
const hre = require("hardhat");

async function main() {
    await deployTokens();
}

async function deployTokens() {

    const tokens = [
        ["Gold Tethered", "GLDT"],
        ["Silver Tethered", "SLVT"],
    ];

    for(let i=0; i<tokens.length; i++) {
        const token = tokens[i]
        const TestToken = await hre.ethers.getContractFactory("TestToken");
        const testToken = await TestToken.deploy(token[0], token[1]);
        await testToken.deployed();

        console.log(token[0] + " (" + token[1] + ") Token deployed to:", testToken.address);

        const accounts = await ethers.getSigners();
        const owner = accounts[0];
        const share = (await testToken.totalSupply()).div(20);

        for(j=1; j<20; j++) {
            await testToken.transfer(accounts[j].address, share)
        }
    }
}

main()
  .then(() => process.exit(0))
  .catch((error) => {
    console.error(error);
    process.exit(1);
  });

