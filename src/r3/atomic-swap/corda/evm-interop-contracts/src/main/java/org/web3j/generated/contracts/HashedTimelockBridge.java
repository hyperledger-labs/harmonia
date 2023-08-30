package org.web3j.generated.contracts;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple5;
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
public class HashedTimelockBridge extends Contract {
    public static final String BINARY = "608060405234801561001057600080fd5b5061108e806100206000396000f3fe608060405234801561001057600080fd5b50600436106100625760003560e01c806309ccb7d31461006757806346ff92431461007c57806359e700041461008f578063b896e695146100b5578063d501845d146100c8578063f46e5928146100db575b600080fd5b61007a610075366004610b47565b6100ff565b005b61007a61008a366004610bdd565b61037f565b6100a261009d366004610c37565b6104ed565b6040519081526020015b60405180910390f35b61007a6100c3366004610c37565b610547565b6100a26100d6366004610c79565b610708565b6100ee6100e9366004610b47565b61073c565b6040516100ac959493929190610cbb565b846001600160a01b03811661012f5760405162461bcd60e51b815260040161012690610d28565b60405180910390fd5b846001600160a01b0381166101565760405162461bcd60e51b815260040161012690610d28565b84600081116101775760405162461bcd60e51b815260040161012690610d5f565b6000610185898989896104ed565b9050866000036101a857604051637bc90c0560e11b815260040160405180910390fd5b6001600160a01b0388166101cf5760405163c872138960e01b815260040160405180910390fd5b6001600160a01b03881633036101f85760405163789f3c0d60e11b815260040160405180910390fd5b6000818152602081905260409020546001600160a01b03161561022e576040516317c3335f60e21b815260040160405180910390fd5b42851161024e5760405163159ff8cf60e21b815260040160405180910390fd5b6040805160e0810182523381526001600160a01b038a811660208084019182528d8316848601908152606085018d8152608086018c815260a087018e815288518086018a52600080825260c08a019182528b81529586905298909420875181549088166001600160a01b031991821617825595516001820180549189169188169190911790559251600284018054919097169516949094179094559251600384015590516004830155516005820155915190919060068201906103119082610e35565b509050506103218933308a6108cb565b604080516001600160a01b03808c1682528a166020820152908101889052606081018790527fce5aa80d5a0785faf7555aaebaff45f1d50c3976c94d46f53ee2528b1486dc15906080015b60405180910390a1505050505050505050565b836001600160a01b0381166103a65760405162461bcd60e51b815260040161012690610d28565b83600081116103c75760405162461bcd60e51b815260040161012690610d5f565b60006103d38585610708565b905060006103e3883389856104ed565b60008181526020819052604090208054919250906001600160a01b031661041d57604051636086ee6f60e11b815260040160405180910390fd5b600181015481546001600160a01b0391821691160361044f5760405163e588959b60e01b815260040160405180910390fd5b80600301546000036104745760405163372fb9b160e11b815260040160405180910390fd5b4281600401541115610499576040516307b7d7dd60e51b815260040160405180910390fd5b60006003820155600681016104af878983610ef5565b506104bc8930338b6108cb565b7f9ccc731d2d6677938f0397c5d3f76c4532e7e7a73fd55d1258e8b78023c217d4878760405161036c929190610fb6565b6040516bffffffffffffffffffffffff19606086811b8216602084015285901b1660348201526048810183905260688101829052600090608801604051602081830303815290604052805190602001209050949350505050565b836001600160a01b03811661056e5760405162461bcd60e51b815260040161012690610d28565b826000811161058f5760405162461bcd60e51b815260040161012690610d5f565b600061059d878787876104ed565b600081815260208190526040902060048101549192509042116105d3576040516307b7d7dd60e51b815260040160405180910390fd5b80546001600160a01b03166105fb57604051636086ee6f60e11b815260040160405180910390fd5b600181015481546001600160a01b0391821691160361062d5760405163e588959b60e01b815260040160405180910390fd5b80600301546000036106525760405163372fb9b160e11b815260040160405180910390fd5b42816004015411610676576040516308250dcf60e11b815260040160405180910390fd5b6000600382015580546001820180546001600160a01b0319166001600160a01b039092169190911790556106ac883033896108cb565b604080516001600160a01b03808b16825289166020820152908101879052606081018690527f9e3d97b3b0c110adac8ce8c4bff2da0e3ab9d737ebedf97fc3338183bb480a879060800160405180910390a15050505050505050565b6000828260405160200161071d929190610fe5565b6040516020818303038152906040528051906020012090505b92915050565b600080606060008060006107528b8b8b8b6104ed565b6000818152602081815260408083208151928301909152828252600481015481549399509197509095504294509192506001600160a01b03161580156107985750828810155b156107a75760005b96506108bc565b80546001600160a01b03166107bd5760056107a0565b805460018201546001600160a01b039182169116036107dd5760046107a0565b806003015460000361088157600296508060060180546107fc90610dac565b80601f016020809104026020016040519081016040528092919081815260200182805461082890610dac565b80156108755780601f1061084a57610100808354040283529160200191610875565b820191906000526020600020905b81548152906001019060200180831161085857829003601f168201915b505050505094506108bc565b828160040154106108a7576001808201549097506001600160a01b0316331495506108bc565b600381549097506001600160a01b0316331495505b50509550955095509550959050565b816001600160a01b0316836001600160a01b0316148061090857506001600160a01b038316301480159061090857506001600160a01b0382163014155b156109265760405163293adf9760e01b815260040160405180910390fd5b6040516370a0823160e01b815230600482015284906000906001600160a01b038316906370a0823190602401602060405180830381865afa15801561096f573d6000803e3d6000fd5b505050506040513d601f19601f820116820180604052508101906109939190610ff5565b6040516323b872dd60e01b81523360048201526001600160a01b03868116602483015260448201869052919250908316906323b872dd906064016020604051808303816000875af1925050508015610a08575060408051601f3d908101601f19168201909252610a059181019061100e565b60015b610a2f57604051634ae43f1760e01b81526004810184905260006024820152604401610126565b801515600003610a5c57604051634ae43f1760e01b81526004810185905260006024820152604401610126565b506040516370a0823160e01b81523060048201526000906001600160a01b038416906370a0823190602401602060405180830381865afa158015610aa4573d6000803e3d6000fd5b505050506040513d601f19601f82011682018060405250810190610ac89190610ff5565b905060006001600160a01b0387163014610aeb57610ae68383611037565b610af5565b610af58284611037565b9050808514610b2157604051634ae43f1760e01b81526004810186905260248101829052604401610126565b5050505050505050565b80356001600160a01b0381168114610b4257600080fd5b919050565b600080600080600060a08688031215610b5f57600080fd5b610b6886610b2b565b9450610b7660208701610b2b565b94979496505050506040830135926060810135926080909101359150565b60008083601f840112610ba657600080fd5b50813567ffffffffffffffff811115610bbe57600080fd5b602083019150836020828501011115610bd657600080fd5b9250929050565b60008060008060608587031215610bf357600080fd5b610bfc85610b2b565b935060208501359250604085013567ffffffffffffffff811115610c1f57600080fd5b610c2b87828801610b94565b95989497509550505050565b60008060008060808587031215610c4d57600080fd5b610c5685610b2b565b9350610c6460208601610b2b565b93969395505050506040820135916060013590565b60008060208385031215610c8c57600080fd5b823567ffffffffffffffff811115610ca357600080fd5b610caf85828601610b94565b90969095509350505050565b858152600060208615158184015260a0604084015285518060a085015260005b81811015610cf75787810183015185820160c001528201610cdb565b50600060c0828601015260c0601f19601f830116850101925050508360608301528260808301529695505050505050565b60208082526017908201527f416464726573732063616e6e6f74206265206e756c6c2e000000000000000000604082015260600190565b6020808252601e908201527f416d6f756e74206d7573742062652067726561746572207468616e20302e0000604082015260600190565b634e487b7160e01b600052604160045260246000fd5b600181811c90821680610dc057607f821691505b602082108103610de057634e487b7160e01b600052602260045260246000fd5b50919050565b601f821115610e3057600081815260208120601f850160051c81016020861015610e0d5750805b601f850160051c820191505b81811015610e2c57828155600101610e19565b5050505b505050565b815167ffffffffffffffff811115610e4f57610e4f610d96565b610e6381610e5d8454610dac565b84610de6565b602080601f831160018114610e985760008415610e805750858301515b600019600386901b1c1916600185901b178555610e2c565b600085815260208120601f198616915b82811015610ec757888601518255948401946001909101908401610ea8565b5085821015610ee55787850151600019600388901b60f8161c191681555b5050505050600190811b01905550565b67ffffffffffffffff831115610f0d57610f0d610d96565b610f2183610f1b8354610dac565b83610de6565b6000601f841160018114610f555760008515610f3d5750838201355b600019600387901b1c1916600186901b178355610faf565b600083815260209020601f19861690835b82811015610f865786850135825560209485019460019092019101610f66565b5086821015610fa35760001960f88860031b161c19848701351681555b505060018560011b0183555b5050505050565b60208152816020820152818360408301376000818301604090810191909152601f909201601f19160101919050565b8183823760009101908152919050565b60006020828403121561100757600080fd5b5051919050565b60006020828403121561102057600080fd5b8151801515811461103057600080fd5b9392505050565b8181038181111561073657634e487b7160e01b600052601160045260246000fdfea2646970667358221220923049635ae515aedd0578c1efae8c9a12e65c0966daf3dc90f8c4a6a5670eda64736f6c63430008100033";

    public static final String FUNC_DEPOSITSTATUS = "depositStatus";

    public static final String FUNC_GETHASH = "getHash";

    public static final String FUNC_HASHFN = "hashfn";

    public static final String FUNC_LOCKTOKENS = "lockTokens";

    public static final String FUNC_REVERTTOKENS = "revertTokens";

    public static final String FUNC_UNLOCKTOKENS = "unlockTokens";

    public static final Event TOKENSLOCKED_EVENT = new Event("TokensLocked", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Address>() {}, new TypeReference<Uint256>() {}, new TypeReference<Bytes32>() {}));
    ;

    public static final Event TOKENSREVERTED_EVENT = new Event("TokensReverted", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Address>() {}, new TypeReference<Uint256>() {}, new TypeReference<Bytes32>() {}));
    ;

    public static final Event TOKENSUNLOCKED_EVENT = new Event("TokensUnlocked", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
    ;

    @Deprecated
    protected HashedTimelockBridge(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected HashedTimelockBridge(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected HashedTimelockBridge(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected HashedTimelockBridge(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public List<TokensLockedEventResponse> getTokensLockedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(TOKENSLOCKED_EVENT, transactionReceipt);
        ArrayList<TokensLockedEventResponse> responses = new ArrayList<TokensLockedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            TokensLockedEventResponse typedResponse = new TokensLockedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.erc20Address = (String) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.to = (String) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
            typedResponse.hash = (byte[]) eventValues.getNonIndexedValues().get(3).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<TokensLockedEventResponse> tokensLockedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, TokensLockedEventResponse>() {
            @Override
            public TokensLockedEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(TOKENSLOCKED_EVENT, log);
                TokensLockedEventResponse typedResponse = new TokensLockedEventResponse();
                typedResponse.log = log;
                typedResponse.erc20Address = (String) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.to = (String) eventValues.getNonIndexedValues().get(1).getValue();
                typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
                typedResponse.hash = (byte[]) eventValues.getNonIndexedValues().get(3).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<TokensLockedEventResponse> tokensLockedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(TOKENSLOCKED_EVENT));
        return tokensLockedEventFlowable(filter);
    }

    public List<TokensRevertedEventResponse> getTokensRevertedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(TOKENSREVERTED_EVENT, transactionReceipt);
        ArrayList<TokensRevertedEventResponse> responses = new ArrayList<TokensRevertedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            TokensRevertedEventResponse typedResponse = new TokensRevertedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.erc20Address = (String) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.to = (String) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
            typedResponse.hash = (byte[]) eventValues.getNonIndexedValues().get(3).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<TokensRevertedEventResponse> tokensRevertedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, TokensRevertedEventResponse>() {
            @Override
            public TokensRevertedEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(TOKENSREVERTED_EVENT, log);
                TokensRevertedEventResponse typedResponse = new TokensRevertedEventResponse();
                typedResponse.log = log;
                typedResponse.erc20Address = (String) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.to = (String) eventValues.getNonIndexedValues().get(1).getValue();
                typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
                typedResponse.hash = (byte[]) eventValues.getNonIndexedValues().get(3).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<TokensRevertedEventResponse> tokensRevertedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(TOKENSREVERTED_EVENT));
        return tokensRevertedEventFlowable(filter);
    }

    public List<TokensUnlockedEventResponse> getTokensUnlockedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(TOKENSUNLOCKED_EVENT, transactionReceipt);
        ArrayList<TokensUnlockedEventResponse> responses = new ArrayList<TokensUnlockedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            TokensUnlockedEventResponse typedResponse = new TokensUnlockedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.secret = (String) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<TokensUnlockedEventResponse> tokensUnlockedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, TokensUnlockedEventResponse>() {
            @Override
            public TokensUnlockedEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(TOKENSUNLOCKED_EVENT, log);
                TokensUnlockedEventResponse typedResponse = new TokensUnlockedEventResponse();
                typedResponse.log = log;
                typedResponse.secret = (String) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<TokensUnlockedEventResponse> tokensUnlockedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(TOKENSUNLOCKED_EVENT));
        return tokensUnlockedEventFlowable(filter);
    }

    public RemoteFunctionCall<Tuple5<BigInteger, Boolean, String, BigInteger, BigInteger>> depositStatus(String erc20Address, String to, BigInteger amount, byte[] hash, BigInteger timeout) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_DEPOSITSTATUS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, erc20Address), 
                new org.web3j.abi.datatypes.Address(160, to), 
                new org.web3j.abi.datatypes.generated.Uint256(amount), 
                new org.web3j.abi.datatypes.generated.Bytes32(hash), 
                new org.web3j.abi.datatypes.generated.Uint256(timeout)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Bool>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
        return new RemoteFunctionCall<Tuple5<BigInteger, Boolean, String, BigInteger, BigInteger>>(function,
                new Callable<Tuple5<BigInteger, Boolean, String, BigInteger, BigInteger>>() {
                    @Override
                    public Tuple5<BigInteger, Boolean, String, BigInteger, BigInteger> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple5<BigInteger, Boolean, String, BigInteger, BigInteger>(
                                (BigInteger) results.get(0).getValue(), 
                                (Boolean) results.get(1).getValue(), 
                                (String) results.get(2).getValue(), 
                                (BigInteger) results.get(3).getValue(), 
                                (BigInteger) results.get(4).getValue());
                    }
                });
    }

    public RemoteFunctionCall<byte[]> getHash(String erc20Address, String to, BigInteger amount, byte[] hash) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_GETHASH, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, erc20Address), 
                new org.web3j.abi.datatypes.Address(160, to), 
                new org.web3j.abi.datatypes.generated.Uint256(amount), 
                new org.web3j.abi.datatypes.generated.Bytes32(hash)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<byte[]> hashfn(String secret) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_HASHFN, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(secret)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<TransactionReceipt> lockTokens(String erc20Address, String receiver, BigInteger amount, byte[] hash, BigInteger timeout) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_LOCKTOKENS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, erc20Address), 
                new org.web3j.abi.datatypes.Address(160, receiver), 
                new org.web3j.abi.datatypes.generated.Uint256(amount), 
                new org.web3j.abi.datatypes.generated.Bytes32(hash), 
                new org.web3j.abi.datatypes.generated.Uint256(timeout)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> revertTokens(String erc20Address, String to, BigInteger amount, byte[] hash) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_REVERTTOKENS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, erc20Address), 
                new org.web3j.abi.datatypes.Address(160, to), 
                new org.web3j.abi.datatypes.generated.Uint256(amount), 
                new org.web3j.abi.datatypes.generated.Bytes32(hash)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> unlockTokens(String erc20Address, BigInteger amount, String secret) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_UNLOCKTOKENS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, erc20Address), 
                new org.web3j.abi.datatypes.generated.Uint256(amount), 
                new org.web3j.abi.datatypes.Utf8String(secret)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static HashedTimelockBridge load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new HashedTimelockBridge(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static HashedTimelockBridge load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new HashedTimelockBridge(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static HashedTimelockBridge load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new HashedTimelockBridge(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static HashedTimelockBridge load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new HashedTimelockBridge(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<HashedTimelockBridge> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(HashedTimelockBridge.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<HashedTimelockBridge> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(HashedTimelockBridge.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    public static RemoteCall<HashedTimelockBridge> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(HashedTimelockBridge.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<HashedTimelockBridge> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(HashedTimelockBridge.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }

    public static class TokensLockedEventResponse extends BaseEventResponse {
        public String erc20Address;

        public String to;

        public BigInteger amount;

        public byte[] hash;
    }

    public static class TokensRevertedEventResponse extends BaseEventResponse {
        public String erc20Address;

        public String to;

        public BigInteger amount;

        public byte[] hash;
    }

    public static class TokensUnlockedEventResponse extends BaseEventResponse {
        public String secret;
    }
}
