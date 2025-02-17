import axios from "axios";

const endPointUrl = (actor: string) => {
  switch (actor) {
    case "alice":
      return "http://localhost:50005";
    case "bob":
      return "http://localhost:50006";
  }
};

export const generateAsset = async (actor: string, assetName: string) => {
  return axios.post(`${endPointUrl(actor)}/generate-asset`, { assetName });
};

export const getAllAssets = async (actor: string) => {
  return axios.get(`${endPointUrl(actor)}/all-assets`);
};

export const whoAmI = async (actor: string) => {
  return axios.get(`${endPointUrl(actor)}/me`);
};

export const createDraftAssetSwap = async (
  actor: string,
  txHash: string,
  txIndex: number,
  recipient: string,
  validators: string[],
  signaturesRequired: number,
  contractAddress: string,
  fromAddress: string,
  toAddress: string,
  evmAmount: string,
) => {
  return axios.post(`${endPointUrl(actor)}/draft-asset-swap`, {
    txHash,
    txIndex,
    recipient,
    validators,
    signaturesRequired,
    contractAddress,
    fromAddress,
    toAddress,
    evmAmount,
  });
};

export const signDraftTransaction = async (
  actor: string,
  draftTxHash: string,
) => {
  return axios.post(`${endPointUrl(actor)}/sign-draft-transaction`, {
    draftTxHash,
  });
};

export const transferAndProve = async (
  actor: string,
  amount: string,
  toAddress: string,
  contractAddress: string,
) => {
  return axios.post(`${endPointUrl(actor)}/transfer-and-prove`, {
    amount,
    toAddress,
    contractAddress,
  });
};

export const collectBlockSignatures = async (
  actor: string,
  draftTxHash: string,
  blockNumber: number,
  blocking: boolean,
) => {
  return axios.post(`${endPointUrl(actor)}/collect-block-signatures`, {
    draftTxHash,
    blockNumber,
    blocking,
  });
};

export const unlockAsset = async (
  actor: string,
  signedTransactionId: string,
  blockNumber: number,
  transactionIndex: number,
) => {
  return axios.post(`${endPointUrl(actor)}/unlock-asset`, {
    signedTransactionId,
    blockNumber,
    transactionIndex,
  });
};
