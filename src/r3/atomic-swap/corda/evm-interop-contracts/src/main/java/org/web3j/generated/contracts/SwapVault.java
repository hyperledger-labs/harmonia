package org.web3j.generated.contracts;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the 
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 4.9.0.
 */
@SuppressWarnings("rawtypes")
public class SwapVault extends Contract {
    public static final String BINARY = "608060405234801561001057600080fd5b5061123e806100206000396000f3fe608060405234801561001057600080fd5b50600436106100625760003560e01c80630352da42146100675780631a0becd51461007c5780635143407e1461008f5780635e943153146100a2578063c07bdccc146100b5578063ea6e0b53146100c8575b600080fd5b61007a610075366004610d15565b6100ed565b005b61007a61008a366004610d97565b610375565b61007a61009d366004610dd9565b610582565b61007a6100b0366004610e50565b610623565b61007a6100c3366004610d97565b610636565b6100db6100d6366004610d97565b6107fa565b60405190815260200160405180910390f35b60006100fb888885856108ac565b6004818101869055600582018790556006820180546001600160a01b0319166001600160a01b038a169081179091556040516301ffc9a760e01b81526380ac58cd60e01b928101929092529192506301ffc9a790602401602060405180830381865afa15801561016f573d6000803e3d6000fd5b505050506040513d601f19601f820116820180604052508101906101939190610eac565b156102245780600401546001146101bd5760405163fae8279160e01b815260040160405180910390fd5b6040516323b872dd60e01b81526001600160a01b038716906323b872dd906101ed90339030908a90600401610ece565b600060405180830381600087803b15801561020757600080fd5b505af115801561021b573d6000803e3d6000fd5b50505050610317565b6040516301ffc9a760e01b8152636cdb3d1360e11b60048201526001600160a01b038716906301ffc9a790602401602060405180830381865afa15801561026f573d6000803e3d6000fd5b505050506040513d601f19601f820116820180604052508101906102939190610eac565b156102fe576001816004015410156102be5760405163fae8279160e01b815260040160405180910390fd5b60408051602081018252600081529051637921219560e11b81526001600160a01b0388169163f242432a916101ed91339130918b918b9190600401610f16565b604051633054c3bd60e21b815260040160405180910390fd5b8787604051610327929190610f71565b60405180910390207fff3ad807565a2fde3b0ea90779fa59b43b9b80799bed0490f89cc8d0838f674461035a8a8a6107fa565b60405190815260200160405180910390a25050505050505050565b6000808383604051610388929190610f71565b90815260405190819003602001902060068101546001820180549293506001600160a01b0390911691600091826103be83610f81565b919050559050806001146103e55760405163343b80b160e01b815260040160405180910390fd5b60028301546001600160a01b03163314610412576040516318ce148560e01b815260040160405180910390fd5b610423826380ac58cd60e01b6109a7565b1561049a57600383015460058401546040516323b872dd60e01b81526001600160a01b03858116936323b872dd9361046393309390921691600401610ece565b600060405180830381600087803b15801561047d57600080fd5b505af1158015610491573d6000803e3d6000fd5b50505050610527565b6104ab82636cdb3d1360e11b6109a7565b15610501576003830154600584015460048086015460408051602081018252600081529051637921219560e11b81526001600160a01b038881169663f242432a9661046396309693909216949093909101610f16565b600683015460038401546004850154610527926001600160a01b03908116921690610a7f565b8484604051610537929190610f71565b60405180910390207f8e1c19a9c47e933c47069384406cbca1c87e51f624596f413819af4dd440038d61056a87876107fa565b60405190815260200160405180910390a25050505050565b6000610590878785856108ac565b600481018590556006810180546001600160a01b0319166001600160a01b0388169081179091559091506105c690333087610ae3565b86866040516105d6929190610f71565b60405180910390207fff3ad807565a2fde3b0ea90779fa59b43b9b80799bed0490f89cc8d0838f674461060989896107fa565b60405190815260200160405180910390a250505050505050565b61062f848484846108ac565b5050505050565b6000808383604051610649929190610f71565b90815260405190819003602001902060068101546001820180549293506001600160a01b03909116916000918261067f83610f81565b919050559050806001146106a65760405163343b80b160e01b815260040160405180910390fd5b6106b7826380ac58cd60e01b6109a7565b1561072e57600283015460058401546040516323b872dd60e01b81526001600160a01b03858116936323b872dd936106f793309390921691600401610ece565b600060405180830381600087803b15801561071157600080fd5b505af1158015610725573d6000803e3d6000fd5b505050506107bb565b61073f82636cdb3d1360e11b6109a7565b15610795576002830154600584015460048086015460408051602081018252600081529051637921219560e11b81526001600160a01b038881169663f242432a966106f796309693909216949093909101610f16565b6006830154600284015460048501546107bb926001600160a01b03908116921690610a7f565b84846040516107cb929190610f71565b60405180910390207ff3702e9dfcd6068628d686f7cbab30d8f1a742f2ab8c551ad3b4e0056370b60d61056a87875b6000806000848460405161080f929190610f71565b908152602001604051809103902090506001816001015410156108455760405163343b80b160e01b815260040160405180910390fd5b60028101546003820154600483015460058401546006850154600786015460405161088d96469689966001600160a01b03928316969183169590949193921691602001610fe2565b6040516020818303038152906040528051906020012091505092915050565b60008085856040516108bf929190610f71565b9081526020016040518091039020905060008160010160008154809291906108e690610f81565b9091555090506001600160a01b0384166109135760405163538ba4f960e01b815260040160405180910390fd5b336001600160a01b0385160361093c5760405163fd298a0960e01b815260040160405180910390fd5b801561095b576040516358595da360e01b815260040160405180910390fd5b8161096786888361112c565b5050600281018054336001600160a01b0319918216179091556003820180549091166001600160a01b039490941693909317909255600782015592915050565b604080516001600160e01b0319831660248083019190915282518083039091018152604490910182526020810180516001600160e01b03166301ffc9a760e01b1790529051600091829182916001600160a01b03871691610a0891906111ec565b600060405180830381855afa9150503d8060008114610a43576040519150601f19603f3d011682016040523d82523d6000602084013e610a48565b606091505b5091509150818015610a5b575060008151115b8015610a76575080806020019051810190610a769190610eac565b95945050505050565b6040516001600160a01b03838116602483015260448201839052610ade91859182169063a9059cbb906064015b604051602081830303815290604052915060e01b6020820180516001600160e01b038381831617835250505050610b11565b505050565b610b0b84856001600160a01b03166323b872dd868686604051602401610aac93929190610ece565b50505050565b6000610b266001600160a01b03841683610b79565b90508051600014158015610b4b575080806020019051810190610b499190610eac565b155b15610ade57604051635274afe760e01b81526001600160a01b03841660048201526024015b60405180910390fd5b6060610b8783836000610b8e565b9392505050565b606081471015610bb35760405163cd78605960e01b8152306004820152602401610b70565b600080856001600160a01b03168486604051610bcf91906111ec565b60006040518083038185875af1925050503d8060008114610c0c576040519150601f19603f3d011682016040523d82523d6000602084013e610c11565b606091505b5091509150610c21868383610c2b565b9695505050505050565b606082610c4057610c3b82610c87565b610b87565b8151158015610c5757506001600160a01b0384163b155b15610c8057604051639996b31560e01b81526001600160a01b0385166004820152602401610b70565b5080610b87565b805115610c975780518082602001fd5b604051630a12f52160e11b815260040160405180910390fd5b60008083601f840112610cc257600080fd5b50813567ffffffffffffffff811115610cda57600080fd5b602083019150836020828501011115610cf257600080fd5b9250929050565b80356001600160a01b0381168114610d1057600080fd5b919050565b600080600080600080600060c0888a031215610d3057600080fd5b873567ffffffffffffffff811115610d4757600080fd5b610d538a828b01610cb0565b9098509650610d66905060208901610cf9565b94506040880135935060608801359250610d8260808901610cf9565b915060a0880135905092959891949750929550565b60008060208385031215610daa57600080fd5b823567ffffffffffffffff811115610dc157600080fd5b610dcd85828601610cb0565b90969095509350505050565b60008060008060008060a08789031215610df257600080fd5b863567ffffffffffffffff811115610e0957600080fd5b610e1589828a01610cb0565b9097509550610e28905060208801610cf9565b935060408701359250610e3d60608801610cf9565b9150608087013590509295509295509295565b60008060008060608587031215610e6657600080fd5b843567ffffffffffffffff811115610e7d57600080fd5b610e8987828801610cb0565b9095509350610e9c905060208601610cf9565b9396929550929360400135925050565b600060208284031215610ebe57600080fd5b81518015158114610b8757600080fd5b6001600160a01b039384168152919092166020820152604081019190915260600190565b60005b83811015610f0d578181015183820152602001610ef5565b50506000910152565b600060018060a01b03808816835280871660208401525084604083015283606083015260a0608083015282518060a0840152610f598160c0850160208701610ef2565b601f01601f19169190910160c0019695505050505050565b8183823760009101908152919050565b600060018201610fa157634e487b7160e01b600052601160045260246000fd5b5060010190565b600181811c90821680610fbc57607f821691505b602082108103610fdc57634e487b7160e01b600052602260045260246000fd5b50919050565b60006101008a83526020818185015260008b54610ffe81610fa8565b9386018490526101209360018281168015611020576001811461103a57611068565b60ff1984168988015282151560051b890187019450611068565b8f6000528560002060005b848110156110605781548b82018a0152908301908701611045565b8a0188019550505b5050506001600160a01b038c166040870152509250611085915050565b6001600160a01b03871660608301528560808301528460a08301526110b560c08301856001600160a01b03169052565b8260e08301529998505050505050505050565b634e487b7160e01b600052604160045260246000fd5b601f821115610ade57600081815260208120601f850160051c810160208610156111055750805b601f850160051c820191505b8181101561112457828155600101611111565b505050505050565b67ffffffffffffffff831115611144576111446110c8565b611158836111528354610fa8565b836110de565b6000601f84116001811461118c57600085156111745750838201355b600019600387901b1c1916600186901b17835561062f565b600083815260209020601f19861690835b828110156111bd578685013582556020948501946001909201910161119d565b50868210156111da5760001960f88860031b161c19848701351681555b505060018560011b0183555050505050565b600082516111fe818460208701610ef2565b919091019291505056fea2646970667358221220603808133c49716a4116707e803630904720aeb4c6dc67ab7932952b5a36e42f64736f6c63430008140033";

    public static final String FUNC_CLAIMCOMMITMENT = "claimCommitment";

    public static final String FUNC_COMMIT = "commit";

    public static final String FUNC_commitWithToken = "commitWithToken";

    public static final String FUNC_COMMITMENTHASH = "commitmentHash";

    public static final String FUNC_REVERTCOMMITMENT = "revertCommitment";

    public static final Event COMMIT_EVENT = new Event("Commit", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>(true) {}, new TypeReference<Bytes32>() {}));
    ;

    public static final Event REVERT_EVENT = new Event("Revert", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>(true) {}, new TypeReference<Bytes32>() {}));
    ;

    public static final Event TRANSFER_EVENT = new Event("Transfer", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>(true) {}, new TypeReference<Bytes32>() {}));
    ;

    @Deprecated
    protected SwapVault(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected SwapVault(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected SwapVault(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected SwapVault(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public List<CommitEventResponse> getCommitEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(COMMIT_EVENT, transactionReceipt);
        ArrayList<CommitEventResponse> responses = new ArrayList<CommitEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            CommitEventResponse typedResponse = new CommitEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.swapId = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.holdHash = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<CommitEventResponse> commitEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, CommitEventResponse>() {
            @Override
            public CommitEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(COMMIT_EVENT, log);
                CommitEventResponse typedResponse = new CommitEventResponse();
                typedResponse.log = log;
                typedResponse.swapId = (byte[]) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.holdHash = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<CommitEventResponse> commitEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(COMMIT_EVENT));
        return commitEventFlowable(filter);
    }

    public List<RevertEventResponse> getRevertEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(REVERT_EVENT, transactionReceipt);
        ArrayList<RevertEventResponse> responses = new ArrayList<RevertEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            RevertEventResponse typedResponse = new RevertEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.swapId = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.holdHash = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<RevertEventResponse> revertEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, RevertEventResponse>() {
            @Override
            public RevertEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(REVERT_EVENT, log);
                RevertEventResponse typedResponse = new RevertEventResponse();
                typedResponse.log = log;
                typedResponse.swapId = (byte[]) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.holdHash = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<RevertEventResponse> revertEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(REVERT_EVENT));
        return revertEventFlowable(filter);
    }

    public List<TransferEventResponse> getTransferEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(TRANSFER_EVENT, transactionReceipt);
        ArrayList<TransferEventResponse> responses = new ArrayList<TransferEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            TransferEventResponse typedResponse = new TransferEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.swapId = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.holdHash = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<TransferEventResponse> transferEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, TransferEventResponse>() {
            @Override
            public TransferEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(TRANSFER_EVENT, log);
                TransferEventResponse typedResponse = new TransferEventResponse();
                typedResponse.log = log;
                typedResponse.swapId = (byte[]) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.holdHash = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<TransferEventResponse> transferEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(TRANSFER_EVENT));
        return transferEventFlowable(filter);
    }

    public RemoteFunctionCall<TransactionReceipt> claimCommitment(String swapId) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_CLAIMCOMMITMENT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(swapId)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> commit(String swapId, String recipient, BigInteger signaturesThreshold) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_COMMIT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(swapId), 
                new org.web3j.abi.datatypes.Address(160, recipient), 
                new org.web3j.abi.datatypes.generated.Uint256(signaturesThreshold)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> commitWithToken(String swapId, String tokenAddress, BigInteger tokenId, BigInteger amount, String recipient, BigInteger signaturesThreshold) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_commitWithToken, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(swapId), 
                new org.web3j.abi.datatypes.Address(160, tokenAddress), 
                new org.web3j.abi.datatypes.generated.Uint256(tokenId), 
                new org.web3j.abi.datatypes.generated.Uint256(amount), 
                new org.web3j.abi.datatypes.Address(160, recipient), 
                new org.web3j.abi.datatypes.generated.Uint256(signaturesThreshold)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> commitWithToken(String swapId, String tokenAddress, BigInteger amount, String recipient, BigInteger signaturesThreshold) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_commitWithToken, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(swapId), 
                new org.web3j.abi.datatypes.Address(160, tokenAddress), 
                new org.web3j.abi.datatypes.generated.Uint256(amount), 
                new org.web3j.abi.datatypes.Address(160, recipient), 
                new org.web3j.abi.datatypes.generated.Uint256(signaturesThreshold)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<byte[]> commitmentHash(String swapId) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_COMMITMENTHASH, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(swapId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<TransactionReceipt> revertCommitment(String swapId) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_REVERTCOMMITMENT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(swapId)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static SwapVault load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new SwapVault(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static SwapVault load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new SwapVault(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static SwapVault load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new SwapVault(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static SwapVault load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new SwapVault(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<SwapVault> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(SwapVault.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<SwapVault> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(SwapVault.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    public static RemoteCall<SwapVault> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(SwapVault.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<SwapVault> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(SwapVault.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }

    public static class CommitEventResponse extends BaseEventResponse {
        public byte[] swapId;

        public byte[] holdHash;
    }

    public static class RevertEventResponse extends BaseEventResponse {
        public byte[] swapId;

        public byte[] holdHash;
    }

    public static class TransferEventResponse extends BaseEventResponse {
        public byte[] swapId;

        public byte[] holdHash;
    }
}
