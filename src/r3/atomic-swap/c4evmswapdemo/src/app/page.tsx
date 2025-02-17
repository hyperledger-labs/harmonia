"use client";
import React, { useState, useEffect} from "react";
import {
  Container,
  Stepper,
  Step,
  StepLabel,
  Button,
  Typography,
  CircularProgress,
  Alert,
  TextField,
} from "@mui/material";
import {
  whoAmI,
  generateAsset,
  createDraftAssetSwap,
  signDraftTransaction,
  transferAndProve,
  collectBlockSignatures,
  unlockAsset,
  getAllAssets
} from "@/gateway/api";
import { useSteps } from "@/hooks/useSteps";
import {erc20abi} from "@/abi/erc20";
import Web3 from "web3";
import AssetsTable from "@/components/AssetsTable";
const steps = [
  "Initial State",
  "Generate Corda Asset (Bob)",
  "Draft Asset Swap (Bob)",
  "Sign Draft Transaction (Bob)",
  "Transfer Asset on Ethereum (Alice)",
  "Collect Signatures and Unlock Asset (Bob)",
  "End Balances"
];

const GenerateAsset = ({ assetName, setAssetName }) => {
  return (
    <>
      <TextField
        id="standard-basic"
        label="Asset Name"
        placeholder="Enter Asset Name"
        value={assetName}
        onChange={(e) => setAssetName(e.target.value)}
      />
    </>
  );
};


const BalanceDisplay = ({aliceBalance, bobBalance, tokenName, tokenSymbol}) => {
  return (
    <>
      <Typography variant="h6" gutterBottom style={{color:"black"}}>
        Token Name: {tokenName}
      </Typography>
      <Typography variant="h6" gutterBottom style={{color:"black"}}>
        Token Symbol: {tokenSymbol}
      </Typography>
      <Typography variant="h6" gutterBottom style={{color:"black"}}>
        Alice Balance: {aliceBalance}
      </Typography>
      <Typography variant="h6" gutterBottom style={{color:"black"}}>
        Bob Balance: {bobBalance}
      </Typography>
    </>
  );
}

const EvmPrice = ({evmPrice, setEvmPrice, assetName}) => {
  return (
    <>
      <Typography variant="h6" gutterBottom style={{color:"black", marginBottom: "1em"}}>
        Price for: {assetName}
      </Typography>
      <TextField
        id="standard-basic"
        label="EVM Price"
        placeholder="Enter EVM Price"
        value={evmPrice}
        onChange={(e) => setEvmPrice(e.target.value)}
      />
    </>
  );
}

const CordaBalanceComparison = ({aliceAssets, bobAssets}) => {
  console.log("Alice Assets: ", aliceAssets);
  console.log("Bob Assets: ", bobAssets); 
  return (
    <div style={{display: "flex", flexDirection: "row", justifyContent: "space-around", marginTop: "1em", borderTop: "1px solid black", paddingTop: "2em"}}>
      <div>
        <Typography variant="h6" gutterBottom style={{color:"black"}}>
          Alice Assets
        </Typography>
      <AssetsTable assetRows={aliceAssets} />
      </div>
      <div>
        <Typography variant="h6" gutterBottom style={{color:"black"}}>
          Bob Assets
        </Typography>
      <AssetsTable assetRows={bobAssets} />
    </div>
    </div>
  );
}


const CordaEvmDemo = () => {
  const [draftTxHash, setDraftTxHash] = useState("");
  const [signedTxId, setSignedTxId] = useState("");

  const [assetName, setAssetName] = useState("");

  const [assetTxHash, setAssetTxHash] = useState("");
  const [assetIndex, setAssetIndex] = useState(0);

  const [blockNumber, setBlockNumber] = useState(0);
  const [transactionIndex, setTransactionIndex] = useState(0);



  // balance information
  const [aliceBalance, setAliceBalance] = useState(0);
  const [bobBalance, setBobBalance] = useState(0);
  const [tokenName, setTokenName] = useState("");
  const [tokenSymbol, setTokenSymbol] = useState("");



  // SwapDetails
  const [evmPrice, setEvmPrice] = useState(0);


  // Alice Assets
  const [aliceAssets, setAliceAssets] = useState([]);
  const [bobAssets, setBobAssets] = useState([]);

  const {
    activeStep,
    setActiveStep,
    stepResult,
    setStepResult,
    loading,
    setLoading,
    error,
    setError,
  } = useSteps();
  const handleInputStep = () => {
    switch (activeStep) {
      case 0:
        return (
          <>
          <BalanceDisplay aliceBalance={aliceBalance} bobBalance={bobBalance} tokenName={tokenName} tokenSymbol={tokenSymbol} />
          <CordaBalanceComparison aliceAssets={aliceAssets} bobAssets={bobAssets} />
          </>
        )
      case 1:
        return (
          <GenerateAsset assetName={assetName} setAssetName={setAssetName} />
        );
      case 2:
        return (
          <>
          <Typography variant="h6" gutterBottom style={{color:"black"}}>
            Will Create a draft transaction for asset swap with Charlie being the registered validator, requiring 1 block signature
          </Typography>
          <EvmPrice evmPrice={evmPrice} setEvmPrice={setEvmPrice} assetName={assetName} />
          <CordaBalanceComparison aliceAssets={aliceAssets} bobAssets={bobAssets} />
          </>

        );
      case 6:
        return (
          <>
          <BalanceDisplay aliceBalance={aliceBalance} bobBalance={bobBalance} tokenName={tokenName} tokenSymbol={tokenSymbol} /> 
          <CordaBalanceComparison aliceAssets={aliceAssets} bobAssets={bobAssets} />
          </>
        )
      default:
        return <></>;
    }
  };

  const tokenAddress = "0x5FbDB2315678afecb367f032d93F642f64180aa3"



  const alicePrivateKey = "0x59c6995e998f97a5a0044966f0945389dc9e86dae88c7a8412f4603b6b78690d"
  const bobPrivateKey = "0x5de4111afa1a4b94908f83103eb1f1706367c2e68ca870fc3fb9a804cdab365a"

  const getContractInfo = async () => {
    const web3 = new Web3("http://localhost:8545");
    const aliceAccount = web3.eth.accounts.privateKeyToAccount(alicePrivateKey);
    const bobAccount = web3.eth.accounts.privateKeyToAccount(bobPrivateKey);
    // query the contract
    const contract = new web3.eth.Contract(erc20abi.abi, tokenAddress);
    const name = await contract.methods.name().call();
    console.log(`Name: ${name}`);
    const symbol = await contract.methods.symbol().call();
    console.log(`Symbol: ${symbol}`); 
    const totalSupply = await contract.methods.totalSupply().call(); 
    console.log(`Total Supply: ${totalSupply}`);

    const aliceBalance = await contract.methods.balanceOf(aliceAccount.address).call();
    console.log(`Alice Balance: ${aliceBalance}`);
    const bobBalance = await contract.methods.balanceOf(bobAccount.address).call();
    console.log(`Bob Balance: ${bobBalance}`);

    setAliceBalance(aliceBalance);
    setBobBalance(bobBalance);
    setTokenName(name);
    setTokenSymbol(symbol);
  }
  const retrieveAllAssets = async () => {
    const [assets, bobAssets] = await Promise.all([await getAllAssets("alice"), await getAllAssets("bob")]);
    console.log(assets);
    setAliceAssets(assets.data);
    setBobAssets(bobAssets.data);
    console.log(bobAssets); 
  }

  useEffect(() => {
    retrieveAllAssets();
    getContractInfo();
  }, [])
  


  const handleNext = async () => {
    setLoading(true);
    setError("");
    try {
      switch (activeStep) {
        case 0:
          await whoAmI("alice");
          setStepResult("Connected to nodes successfully.");
          break;
        case 1:
          const response = await generateAsset("bob", assetName);
          await retrieveAllAssets();
          const [txHash, txIndex] = response.data.replace(")", "").split("(");
          setAssetTxHash(txHash);
          setAssetIndex(parseInt(txIndex));

          setStepResult(`Asset "${assetName}" generated successfully.`);
          break;
        case 2:
          const d = await createDraftAssetSwap(
            "bob",
            assetTxHash,
            assetIndex,
            "O=Alice,L=London,C=GB",
            ["O=Bob,L=San Francisco,C=US", "O=Charlie,L=Mumbai,C=IN"],
            1,
            tokenAddress,
            "0x70997970C51812dc3A010C7d01b50e0d17dc79C8",
            "0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC",
            evmPrice,
          );
          console.log(d);
          setDraftTxHash(d.data);
          setStepResult("Draft Asset Swap created with hash: " + d.data);
          break;
        case 3:
          const s = await signDraftTransaction("bob", draftTxHash);
          console.log("s: ", s);
          setSignedTxId(s.data);
          setStepResult(`Signed Draft Transaction ${s.data}`);
          break;
        case 4:
          const transfer = await transferAndProve(
            "alice",
            evmPrice,
            "0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC",
            tokenAddress,
          );
          console.log(transfer.data);
          setBlockNumber(transfer.data.blockNumber);
          setTransactionIndex(transfer.data.transactionIndex);
          setStepResult(`Transfered asset to Bob on Ethereum, at block ${transfer.data.blockNumber} and transaction index ${transfer.data.transactionIndex}`);
          break;
        case 5:
          await collectBlockSignatures("bob", draftTxHash, blockNumber, true);
        // wait one second to ensure the transaction is mined
          await new Promise((resolve) => setTimeout(resolve, 1000));
          await unlockAsset("bob", signedTxId, blockNumber, transactionIndex);
          await getContractInfo()
          await retrieveAllAssets()
          setStepResult("Collected signatures and unlocked asset."); 
          break;
        default:
          break;
      }
      setActiveStep((prev) => prev + 1);
    } catch (err) {
      setError(err.message || "An error occurred");
    }
    setLoading(false);
  };

  const handleReset = () => {
    setActiveStep(0);
    setDraftTxHash("");
    setSignedTxId("");
  };

  console.log(erc20abi)

  return (
    <Container style={{ backgroundColor: "#F0EFE9", minHeight: "100vh", minWidth: "100vw", paddingBottom: "3em" }}>
      <Typography variant="h4" gutterBottom style={{color: "black", padding: "1em"}}>
        Corda EVM Demo
      </Typography>
      <Stepper activeStep={activeStep} alternativeLabel>
        {steps.map((label) => (
          <Step key={label}>
            <StepLabel>{label}</StepLabel>
          </Step>
        ))}
      </Stepper>
      {error && <Alert severity="error">{error}</Alert>}
      {activeStep === steps.length ? (
        <>
          <Typography variant="h6" gutterBottom>
            All steps completed - Flow finished successfully
          </Typography>
          <Button onClick={handleReset}>Reset</Button>
        </>
      ) : (
        <>
          <Typography variant="body1" gutterBottom style={{color: "black"}}>
            {steps[activeStep]}
          </Typography>
          <div style={{ padding: 20 }}>{handleInputStep()}</div>
          {stepResult && <Alert severity="info">{stepResult}</Alert>}
          <Button onClick={handleNext} disabled={loading} variant="contained">
            {loading ? <CircularProgress size={24} /> : "Next"}
          </Button>
        </>
      )}
    </Container>
  );
};

export default CordaEvmDemo;

// "use client";
// import { useState } from "react";
// import Image from "next/image";
// import styles from "./page.module.css";
// import Button from "@mui/material/Button";
// import Snackbar, { SnackbarCloseReason } from "@mui/material/Snackbar";
// import IconButton from "@mui/material/IconButton";
// import CloseIcon from "@mui/icons-material/Close";
// import TextField from "@mui/material/TextField";
// import { generateAsset } from "@/gateway/api";
// import { styled } from '@mui/material/styles';
// import Box from '@mui/material/Box';
// import Paper from '@mui/material/Paper';
// import Grid from '@mui/material/Grid2';
// import { Typography } from "@mui/material";
// const Item = styled(Paper)(({ theme }) => ({
//   backgroundColor: '#fff',
//   ...theme.typography.body2,
//   padding: theme.spacing(1),
//   textAlign: 'center',
//   // color: theme.palette.text.secondary,
//   ...theme.applyStyles('dark', {
//     backgroundColor: '#1A2027',
//   }),
// }));

// export default function Home() {
//   const [state, setState] = useState({
//     vertical: "top",
//     horizontal: "right",
//     open: false,
//   });
//   const [assetName, setAssetName] = useState("");

//   const handleClick = () => {
//     setState({ ...state, open: true });
//   };

//   const handleClose = (
//     event: React.SyntheticEvent | Event,
//     reason?: SnackbarCloseReason,
//   ) => {
//     if (reason === "clickaway") {
//       return;
//     }

//     setState({ ...state, open: false });
//   };

//   const issueGenericAsset = async () => {
//     alert(assetName);
//     const assetResponse = await generateAsset(assetName);
//     console.log(assetResponse)
//   }

//   // const action = (
//   //   <>
//   //     <TextField
//   //       id="standard-basic"
//   //       label="Asset Name"
//   //       placeholder="Enter Asset Name"
//   //       value={assetName}
//   //       onChange={(e) => setAssetName(e.target.value)}
//   //     />

//   //     <Button color="secondary" size="small" onClick={issueGenericAsset}>
//   //       Submit
//   //     </Button>
//   //     <IconButton
//   //       size="small"
//   //       aria-label="close"
//   //       color="inherit"
//   //       onClick={handleClose}
//   //     >
//   //       <CloseIcon fontSize="small" />
//   //     </IconButton>
//   //   </>
//   // );

//   // const { open, vertical, horizontal } = state;

//   return (
//     <div style={{backgroundColor:"black", minWidth: "100vh", height: "100vh"}}>

//       <Grid container spacing={2}>
//         <Grid size={3}>
//           <Item>
//             <Typography variant="h6">size=3</Typography>
//           </Item>
//         </Grid>
//         <Grid size={3}>
//           <Item>size=3</Item>
//         </Grid>
//         <Grid size={3}>
//           <Item>size=3</Item>
//         </Grid>
//         <Grid size={3}>
//           <Item>size=3</Item>
//         </Grid>
//         <Grid size={6}>
//           <Item>
//             <Typography variant="h6">Alice</Typography>
//           </Item>
//         </Grid>
//         <Grid size={6}>
//           <Item>
//           <Typography variant="h6">Bob</Typography>
//           </Item>
//         </Grid>

//       </Grid>
//     </div>
//   );
// }
