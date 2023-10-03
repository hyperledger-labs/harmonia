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
import org.web3j.abi.datatypes.Bool;
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
 * <p>Generated with web3j version 4.9.4.
 */
@SuppressWarnings("rawtypes")
public class SwapVault extends Contract {
    public static final String BINARY = "608060405234801561001057600080fd5b50611732806100206000396000f3fe608060405234801561001057600080fd5b506004361061009e5760003560e01c80635e943153116100665780635e9431531461013d578063bc197c8114610150578063c07bdccc1461016f578063ea6e0b5314610182578063f23a6e61146101a357600080fd5b806301ffc9a7146100a35780630352da42146100cb578063150b7a02146100e05780631a0becd5146101175780635143407e1461012a575b600080fd5b6100b66100b1366004610ebc565b6101c2565b60405190151581526020015b60405180910390f35b6100de6100d9366004610f4b565b6101f9565b005b6100fe6100ee366004611084565b630a85bd0160e11b949350505050565b6040516001600160e01b031990911681526020016100c2565b6100de6101253660046110ec565b610481565b6100de61013836600461112e565b61077a565b6100de61014b3660046111a5565b61081b565b6100fe61015e366004611281565b63bc197c8160e01b95945050505050565b6100de61017d3660046110ec565b61082e565b6101956101903660046110ec565b610ade565b6040519081526020016100c2565b6100fe6101b136600461132b565b63f23a6e6160e01b95945050505050565b60006001600160e01b03198216630271189760e51b14806101f357506301ffc9a760e01b6001600160e01b03198316145b92915050565b600061020788888585610b90565b6004818101869055600582018790556006820180546001600160a01b0319166001600160a01b038a169081179091556040516301ffc9a760e01b81526380ac58cd60e01b928101929092529192506301ffc9a790602401602060405180830381865afa15801561027b573d6000803e3d6000fd5b505050506040513d601f19601f8201168201806040525081019061029f9190611390565b156103305780600401546001146102c95760405163fae8279160e01b815260040160405180910390fd5b6040516323b872dd60e01b81526001600160a01b038716906323b872dd906102f990339030908a906004016113b2565b600060405180830381600087803b15801561031357600080fd5b505af1158015610327573d6000803e3d6000fd5b50505050610423565b6040516301ffc9a760e01b8152636cdb3d1360e11b60048201526001600160a01b038716906301ffc9a790602401602060405180830381865afa15801561037b573d6000803e3d6000fd5b505050506040513d601f19601f8201168201806040525081019061039f9190611390565b1561040a576001816004015410156103ca5760405163fae8279160e01b815260040160405180910390fd5b60408051602081018252600081529051637921219560e11b81526001600160a01b0388169163f242432a916102f991339130918b918b91906004016113fa565b604051633054c3bd60e21b815260040160405180910390fd5b8787604051610433929190611455565b60405180910390207fff3ad807565a2fde3b0ea90779fa59b43b9b80799bed0490f89cc8d0838f67446104668a8a610ade565b60405190815260200160405180910390a25050505050505050565b6000808383604051610494929190611455565b90815260405190819003602001902060068101546001820180549293506001600160a01b0390911691600091826104ca83611465565b919050559050806001146104f15760405163343b80b160e01b815260040160405180910390fd5b60028301546001600160a01b0316331461051e576040516318ce148560e01b815260040160405180910390fd5b604051633a47683160e21b815273__$dfea82c46cfe83b183bf0bab00313d62ef$__9063e91da0c490610564906001600160a01b038616906380ac58cd9060040161148c565b602060405180830381865af4158015610581573d6000803e3d6000fd5b505050506040513d601f19601f820116820180604052508101906105a59190611390565b1561061c57600383015460058401546040516323b872dd60e01b81526001600160a01b03858116936323b872dd936105e5933093909216916004016113b2565b600060405180830381600087803b1580156105ff57600080fd5b505af1158015610613573d6000803e3d6000fd5b5050505061071f565b604051633a47683160e21b815273__$dfea82c46cfe83b183bf0bab00313d62ef$__9063e91da0c490610662906001600160a01b0386169063d9b67a269060040161148c565b602060405180830381865af415801561067f573d6000803e3d6000fd5b505050506040513d601f19601f820116820180604052508101906106a39190611390565b156106f9576003830154600584015460048086015460408051602081018252600081529051637921219560e11b81526001600160a01b038881169663f242432a966105e5963096939092169490939091016113fa565b60068301546003840154600485015461071f926001600160a01b03908116921690610c8b565b848460405161072f929190611455565b60405180910390207f8e1c19a9c47e933c47069384406cbca1c87e51f624596f413819af4dd440038d6107628787610ade565b60405190815260200160405180910390a25050505050565b600061078887878585610b90565b600481018590556006810180546001600160a01b0319166001600160a01b0388169081179091559091506107be90333087610cef565b86866040516107ce929190611455565b60405180910390207fff3ad807565a2fde3b0ea90779fa59b43b9b80799bed0490f89cc8d0838f67446108018989610ade565b60405190815260200160405180910390a250505050505050565b61082784848484610b90565b5050505050565b6000808383604051610841929190611455565b90815260405190819003602001902060068101546001820180549293506001600160a01b03909116916000918261087783611465565b9190505590508060011461089e5760405163343b80b160e01b815260040160405180910390fd5b604051633a47683160e21b815273__$dfea82c46cfe83b183bf0bab00313d62ef$__9063e91da0c4906108e4906001600160a01b038616906380ac58cd9060040161148c565b602060405180830381865af4158015610901573d6000803e3d6000fd5b505050506040513d601f19601f820116820180604052508101906109259190611390565b1561099c57600283015460058401546040516323b872dd60e01b81526001600160a01b03858116936323b872dd93610965933093909216916004016113b2565b600060405180830381600087803b15801561097f57600080fd5b505af1158015610993573d6000803e3d6000fd5b50505050610a9f565b604051633a47683160e21b815273__$dfea82c46cfe83b183bf0bab00313d62ef$__9063e91da0c4906109e2906001600160a01b0386169063d9b67a269060040161148c565b602060405180830381865af41580156109ff573d6000803e3d6000fd5b505050506040513d601f19601f82011682018060405250810190610a239190611390565b15610a79576002830154600584015460048086015460408051602081018252600081529051637921219560e11b81526001600160a01b038881169663f242432a96610965963096939092169490939091016113fa565b600683015460028401546004850154610a9f926001600160a01b03908116921690610c8b565b8484604051610aaf929190611455565b60405180910390207ff3702e9dfcd6068628d686f7cbab30d8f1a742f2ab8c551ad3b4e0056370b60d61076287875b60008060008484604051610af3929190611455565b90815260200160405180910390209050600181600101541015610b295760405163343b80b160e01b815260040160405180910390fd5b600281015460038201546004830154600584015460068501546007860154604051610b7196469689966001600160a01b039283169691831695909491939216916020016114ec565b6040516020818303038152906040528051906020012091505092915050565b6000808585604051610ba3929190611455565b908152602001604051809103902090506000816001016000815480929190610bca90611465565b9091555090506001600160a01b038416610bf75760405163538ba4f960e01b815260040160405180910390fd5b336001600160a01b03851603610c205760405163fd298a0960e01b815260040160405180910390fd5b8015610c3f576040516358595da360e01b815260040160405180910390fd5b81610c4b868883611620565b5050600281018054336001600160a01b0319918216179091556003820180549091166001600160a01b039490941693909317909255600782015592915050565b6040516001600160a01b03838116602483015260448201839052610cea91859182169063a9059cbb906064015b604051602081830303815290604052915060e01b6020820180516001600160e01b038381831617835250505050610d1d565b505050565b610d1784856001600160a01b03166323b872dd868686604051602401610cb8939291906113b2565b50505050565b6000610d326001600160a01b03841683610d85565b90508051600014158015610d57575080806020019051810190610d559190611390565b155b15610cea57604051635274afe760e01b81526001600160a01b03841660048201526024015b60405180910390fd5b6060610d9383836000610d9a565b9392505050565b606081471015610dbf5760405163cd78605960e01b8152306004820152602401610d7c565b600080856001600160a01b03168486604051610ddb91906116e0565b60006040518083038185875af1925050503d8060008114610e18576040519150601f19603f3d011682016040523d82523d6000602084013e610e1d565b606091505b5091509150610e2d868383610e37565b9695505050505050565b606082610e4c57610e4782610e93565b610d93565b8151158015610e6357506001600160a01b0384163b155b15610e8c57604051639996b31560e01b81526001600160a01b0385166004820152602401610d7c565b5080610d93565b805115610ea35780518082602001fd5b604051630a12f52160e11b815260040160405180910390fd5b600060208284031215610ece57600080fd5b81356001600160e01b031981168114610d9357600080fd5b60008083601f840112610ef857600080fd5b50813567ffffffffffffffff811115610f1057600080fd5b602083019150836020828501011115610f2857600080fd5b9250929050565b80356001600160a01b0381168114610f4657600080fd5b919050565b600080600080600080600060c0888a031215610f6657600080fd5b873567ffffffffffffffff811115610f7d57600080fd5b610f898a828b01610ee6565b9098509650610f9c905060208901610f2f565b94506040880135935060608801359250610fb860808901610f2f565b915060a0880135905092959891949750929550565b634e487b7160e01b600052604160045260246000fd5b604051601f8201601f1916810167ffffffffffffffff8111828210171561100c5761100c610fcd565b604052919050565b600082601f83011261102557600080fd5b813567ffffffffffffffff81111561103f5761103f610fcd565b611052601f8201601f1916602001610fe3565b81815284602083860101111561106757600080fd5b816020850160208301376000918101602001919091529392505050565b6000806000806080858703121561109a57600080fd5b6110a385610f2f565b93506110b160208601610f2f565b925060408501359150606085013567ffffffffffffffff8111156110d457600080fd5b6110e087828801611014565b91505092959194509250565b600080602083850312156110ff57600080fd5b823567ffffffffffffffff81111561111657600080fd5b61112285828601610ee6565b90969095509350505050565b60008060008060008060a0878903121561114757600080fd5b863567ffffffffffffffff81111561115e57600080fd5b61116a89828a01610ee6565b909750955061117d905060208801610f2f565b93506040870135925061119260608801610f2f565b9150608087013590509295509295509295565b600080600080606085870312156111bb57600080fd5b843567ffffffffffffffff8111156111d257600080fd5b6111de87828801610ee6565b90955093506111f1905060208601610f2f565b9396929550929360400135925050565b600082601f83011261121257600080fd5b8135602067ffffffffffffffff82111561122e5761122e610fcd565b8160051b61123d828201610fe3565b928352848101820192828101908785111561125757600080fd5b83870192505b848310156112765782358252918301919083019061125d565b979650505050505050565b600080600080600060a0868803121561129957600080fd5b6112a286610f2f565b94506112b060208701610f2f565b9350604086013567ffffffffffffffff808211156112cd57600080fd5b6112d989838a01611201565b945060608801359150808211156112ef57600080fd5b6112fb89838a01611201565b9350608088013591508082111561131157600080fd5b5061131e88828901611014565b9150509295509295909350565b600080600080600060a0868803121561134357600080fd5b61134c86610f2f565b945061135a60208701610f2f565b93506040860135925060608601359150608086013567ffffffffffffffff81111561138457600080fd5b61131e88828901611014565b6000602082840312156113a257600080fd5b81518015158114610d9357600080fd5b6001600160a01b039384168152919092166020820152604081019190915260600190565b60005b838110156113f15781810151838201526020016113d9565b50506000910152565b600060018060a01b03808816835280871660208401525084604083015283606083015260a0608083015282518060a084015261143d8160c08501602087016113d6565b601f01601f19169190910160c0019695505050505050565b8183823760009101908152919050565b60006001820161148557634e487b7160e01b600052601160045260246000fd5b5060010190565b6001600160a01b0392909216825260e01b6001600160e01b031916602082015260400190565b600181811c908216806114c657607f821691505b6020821081036114e657634e487b7160e01b600052602260045260246000fd5b50919050565b60006101008a83526020818185015260008b54611508816114b2565b938601849052610120936001828116801561152a576001811461154457611572565b60ff1984168988015282151560051b890187019450611572565b8f6000528560002060005b8481101561156a5781548b82018a015290830190870161154f565b8a0188019550505b5050506001600160a01b038c16604087015250925061158f915050565b6001600160a01b03871660608301528560808301528460a08301526115bf60c08301856001600160a01b03169052565b8260e08301529998505050505050505050565b601f821115610cea57600081815260208120601f850160051c810160208610156115f95750805b601f850160051c820191505b8181101561161857828155600101611605565b505050505050565b67ffffffffffffffff83111561163857611638610fcd565b61164c8361164683546114b2565b836115d2565b6000601f84116001811461168057600085156116685750838201355b600019600387901b1c1916600186901b178355610827565b600083815260209020601f19861690835b828110156116b15786850135825560209485019460019092019101611691565b50868210156116ce5760001960f88860031b161c19848701351681555b505060018560011b0183555050505050565b600082516116f28184602087016113d6565b919091019291505056fea2646970667358221220646cf39780846ce2a4f8fe8a002c7ed6dafbe222fd6f76ee29e395699f408a1e64736f6c63430008140033\n"
            + "\n"
            + "// $dfea82c46cfe83b183bf0bab00313d62ef$ -> src/SwapVault.sol:Utils\n"
            + "// $dfea82c46cfe83b183bf0bab00313d62ef$ -> src/SwapVault.sol:Utils\n"
            + "// $dfea82c46cfe83b183bf0bab00313d62ef$ -> src/SwapVault.sol:Utils\n"
            + "// $dfea82c46cfe8ridge3b183bf0bab00313d62ef$ -> src/SwapVault.sol:Utils";

    public static final String FUNC_CLAIMCOMMITMENT = "claimCommitment";

    public static final String FUNC_COMMIT = "commit";

    public static final String FUNC_commitWithToken = "commitWithToken";

    public static final String FUNC_COMMITMENTHASH = "commitmentHash";

    public static final String FUNC_ONERC1155BATCHRECEIVED = "onERC1155BatchReceived";

    public static final String FUNC_ONERC1155RECEIVED = "onERC1155Received";

    public static final String FUNC_ONERC721RECEIVED = "onERC721Received";

    public static final String FUNC_REVERTCOMMITMENT = "revertCommitment";

    public static final String FUNC_SUPPORTSINTERFACE = "supportsInterface";

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

    public static List<CommitEventResponse> getCommitEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(COMMIT_EVENT, transactionReceipt);
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

    public static List<RevertEventResponse> getRevertEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(REVERT_EVENT, transactionReceipt);
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

    public static List<TransferEventResponse> getTransferEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(TRANSFER_EVENT, transactionReceipt);
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

    public RemoteFunctionCall<TransactionReceipt> onERC1155BatchReceived(String param0, String param1, List<BigInteger> param2, List<BigInteger> param3, byte[] param4) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_ONERC1155BATCHRECEIVED, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, param0), 
                new org.web3j.abi.datatypes.Address(160, param1), 
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.generated.Uint256>(
                        org.web3j.abi.datatypes.generated.Uint256.class,
                        org.web3j.abi.Utils.typeMap(param2, org.web3j.abi.datatypes.generated.Uint256.class)), 
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.generated.Uint256>(
                        org.web3j.abi.datatypes.generated.Uint256.class,
                        org.web3j.abi.Utils.typeMap(param3, org.web3j.abi.datatypes.generated.Uint256.class)), 
                new org.web3j.abi.datatypes.DynamicBytes(param4)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> onERC1155Received(String param0, String param1, BigInteger param2, BigInteger param3, byte[] param4) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_ONERC1155RECEIVED, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, param0), 
                new org.web3j.abi.datatypes.Address(160, param1), 
                new org.web3j.abi.datatypes.generated.Uint256(param2), 
                new org.web3j.abi.datatypes.generated.Uint256(param3), 
                new org.web3j.abi.datatypes.DynamicBytes(param4)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> onERC721Received(String param0, String param1, BigInteger param2, byte[] param3) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_ONERC721RECEIVED, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, param0), 
                new org.web3j.abi.datatypes.Address(160, param1), 
                new org.web3j.abi.datatypes.generated.Uint256(param2), 
                new org.web3j.abi.datatypes.DynamicBytes(param3)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> revertCommitment(String swapId) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_REVERTCOMMITMENT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(swapId)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Boolean> supportsInterface(byte[] interfaceId) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_SUPPORTSINTERFACE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes4(interfaceId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
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
