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
import org.web3j.abi.datatypes.Address;
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
    public static final String BINARY = "608060405234801561001057600080fd5b50611e20806100206000396000f3fe608060405234801561001057600080fd5b50600436106100a95760003560e01c80635e943153116100715780635e9431531461010f57806389449c901461012257806397aba7f9146101355780639ff18eea14610165578063c07bdccc14610178578063ea6e0b531461018b57600080fd5b80630352da42146100ae578063093aaab2146100c35780631a0becd5146100d65780631ffa9854146100e95780635143407e146100fc575b600080fd5b6100c16100bc3660046114f3565b6101ac565b005b6100c16100d13660046115b8565b6101d5565b6100c16100e436600461165e565b610217565b6100c16100f736600461169f565b610255565b6100c161010a366004611751565b6102a3565b6100c161011d3660046117c7565b6102ca565b6100c1610130366004611822565b6102ee565b610148610143366004611963565b61032e565b6040516001600160a01b0390911681526020015b60405180910390f35b6100c16101733660046119a9565b6103d3565b6100c161018636600461165e565b6103ed565b61019e61019936600461165e565b6105cd565b60405190815260200161015c565b6040805160008152602081019091526101cb8888888888888888610682565b5050505050505050565b6101cb88888888888888888080602002602001604051908101604052809392919081815260200183836020028082843760009201919091525061090c92505050565b6040805160008082526020820190925281610242565b606081526020019060019003908161022d5790505b5090506102508383836109af565b505050565b6102988989898989898989898080602002602001604051908101604052809392919081815260200183836020028082843760009201919091525061068292505050565b505050505050505050565b6040805160008152602081019091526102c18787878787878761090c565b50505050505050565b6040805160008152602081019091526102e68585858585610bea565b505050505050565b6102c186868686868680806020026020016040519081016040528093929190818152602001838360200280828437600092019190915250610bea92505050565b6020810151604082015160608301516000929190831a601b811480159061035957508060ff16601c14155b1561036a57600093505050506103cd565b60408051600081526020810180835288905260ff831691810191909152606081018490526080810183905260019060a0016020604051602081039080840390855afa1580156103bd573d6000803e3d6000fd5b5050506020604051035193505050505b92915050565b6103e784846103e28486611a14565b6109af565b50505050565b6000808383604051610400929190611a97565b90815260405190819003602001902060068101546001820180549293506001600160a01b03909116916000918261043683611aa7565b9190505590508060011461045d5760405163343b80b160e01b815260040160405180910390fd5b61046e826380ac58cd60e01b610d1c565b156104e557600283015460058401546040516323b872dd60e01b81526001600160a01b03858116936323b872dd936104ae93309390921691600401611ace565b600060405180830381600087803b1580156104c857600080fd5b505af11580156104dc573d6000803e3d6000fd5b50505050610572565b6104f682636cdb3d1360e11b610d1c565b1561054c576002830154600584015460048086015460408051602081018252600081529051637921219560e11b81526001600160a01b038881169663f242432a966104ae96309693909216949093909101611b42565b600683015460028401546004850154610572926001600160a01b03908116921690610df4565b8484604051610582929190611a97565b60405180910390207ff3702e9dfcd6068628d686f7cbab30d8f1a742f2ab8c551ad3b4e0056370b60d6105b587876105cd565b60405190815260200160405180910390a25050505050565b600080600084846040516105e2929190611a97565b908152602001604051809103902090506001816001015410156106185760405163343b80b160e01b815260040160405180910390fd5b6002810154600382015460048301546005840154600685015460088601546040516106639646966001600160a01b03918216969082169590949093911691600a8a0190602001611b87565b6040516020818303038152906040528051906020012091505092915050565b60006106918989868686610bea565b6004818101879055600582018890556006820180546001600160a01b0319166001600160a01b038b169081179091556040516301ffc9a760e01b81526380ac58cd60e01b928101929092529192506301ffc9a790602401602060405180830381865afa158015610705573d6000803e3d6000fd5b505050506040513d601f19601f820116820180604052508101906107299190611c18565b156107ba5780600401546001146107535760405163fae8279160e01b815260040160405180910390fd5b6040516323b872dd60e01b81526001600160a01b038816906323b872dd9061078390339030908b90600401611ace565b600060405180830381600087803b15801561079d57600080fd5b505af11580156107b1573d6000803e3d6000fd5b505050506108ad565b6040516301ffc9a760e01b8152636cdb3d1360e11b60048201526001600160a01b038816906301ffc9a790602401602060405180830381865afa158015610805573d6000803e3d6000fd5b505050506040513d601f19601f820116820180604052508101906108299190611c18565b15610894576001816004015410156108545760405163fae8279160e01b815260040160405180910390fd5b60408051602081018252600081529051637921219560e11b81526001600160a01b0389169163f242432a9161078391339130918c918c9190600401611b42565b604051633054c3bd60e21b815260040160405180910390fd5b88886040516108bd929190611a97565b60405180910390207fff3ad807565a2fde3b0ea90779fa59b43b9b80799bed0490f89cc8d0838f67446108f08b8b6105cd565b60405190815260200160405180910390a2505050505050505050565b600061091b8888868686610bea565b600481018690556006810180546001600160a01b0319166001600160a01b03891690811790915590915061095190333088610e53565b8787604051610961929190611a97565b60405180910390207fff3ad807565a2fde3b0ea90779fa59b43b9b80799bed0490f89cc8d0838f67446109948a8a6105cd565b60405190815260200160405180910390a25050505050505050565b60008084846040516109c2929190611a97565b90815260405190819003602001902060068101546001820180549293506001600160a01b0390911691600091826109f883611aa7565b91905055905080600114610a1f5760405163343b80b160e01b815260040160405180910390fd5b60028301546001600160a01b03163314801590610a5b575060038301546001600160a01b031633141580610a5b5750610a59868686610e7b565b155b15610a795760405163095f258360e01b815260040160405180910390fd5b610a8a826380ac58cd60e01b610d1c565b15610b0157600383015460058401546040516323b872dd60e01b81526001600160a01b03858116936323b872dd93610aca93309390921691600401611ace565b600060405180830381600087803b158015610ae457600080fd5b505af1158015610af8573d6000803e3d6000fd5b50505050610b8e565b610b1282636cdb3d1360e11b610d1c565b15610b68576003830154600584015460048086015460408051602081018252600081529051637921219560e11b81526001600160a01b038881169663f242432a96610aca96309693909216949093909101611b42565b600683015460038401546004850154610b8e926001600160a01b03908116921690610df4565b8585604051610b9e929190611a97565b60405180910390207f8e1c19a9c47e933c47069384406cbca1c87e51f624596f413819af4dd440038d610bd188886105cd565b60405190815260200160405180910390a2505050505050565b6000808686604051610bfd929190611a97565b908152602001604051809103902090506000816001016000815480929190610c2490611aa7565b9091555090506001600160a01b038516610c515760405163538ba4f960e01b815260040160405180910390fd5b336001600160a01b03861603610c7a5760405163fd298a0960e01b815260040160405180910390fd5b8015610c99576040516358595da360e01b815260040160405180910390fd5b6001841015610cbb57604051633400f7f160e11b815260040160405180910390fd5b81610cc7878983611cba565b50600282018054336001600160a01b0319918216179091556003830180549091166001600160a01b038716179055600882018490558251610d1190600a8401906020860190611415565b505095945050505050565b604080516001600160e01b0319831660248083019190915282518083039091018152604490910182526020810180516001600160e01b03166301ffc9a760e01b1790529051600091829182916001600160a01b03871691610d7d9190611d7a565b600060405180830381855afa9150503d8060008114610db8576040519150601f19603f3d011682016040523d82523d6000602084013e610dbd565b606091505b5091509150818015610dd0575060008151115b8015610deb575080806020019051810190610deb9190611c18565b95945050505050565b6040516001600160a01b0383811660248301526044820183905261025091859182169063a9059cbb906064015b604051602081830303815290604052915060e01b6020820180516001600160e01b0383818316178352505050506112a6565b6103e784856001600160a01b03166323b872dd868686604051602401610e2193929190611ace565b60008060008585604051610e90929190611a97565b908152602001604051809103902060405180610160016040529081600082018054610eba90611c3a565b80601f0160208091040260200160405190810160405280929190818152602001828054610ee690611c3a565b8015610f335780601f10610f0857610100808354040283529160200191610f33565b820191906000526020600020905b815481529060010190602001808311610f1657829003601f168201915b50505091835250506001820154602082015260028201546001600160a01b03908116604083015260038301548116606083015260048301546080830152600583015460a083015260068301541660c082015260078201805460e090920191610f9a90611c3a565b80601f0160208091040260200160405190810160405280929190818152602001828054610fc690611c3a565b80156110135780601f10610fe857610100808354040283529160200191611013565b820191906000526020600020905b815481529060010190602001808311610ff657829003601f168201915b50505050508152602001600882015481526020016009820180548060200260200160405190810160405280929190818152602001828054801561107557602002820191906000526020600020905b815481526020019060010190808311611061575b50505050508152602001600a82018054806020026020016040519081016040528092919081815260200182805480156110d757602002820191906000526020600020905b81546001600160a01b031681526001909101906020018083116110b9575b505050505081525050905080610100015181610120015151101561110e5760405163095f258360e01b815260040160405180910390fd5b6040805160208082018352600080835292519192916111339189918991869101611d96565b6040516020818303038152906040528051906020012090506000836101400151516001600160401b0381111561116b5761116b6118ae565b604051908082528060200260200182016040528015611194578160200160208202803683370190505b5090506000805b875181101561128d5760006111c9858a84815181106111bc576111bc611dd4565b602002602001015161032e565b905060005b84518110156112785787610140015181815181106111ee576111ee611dd4565b60200260200101516001600160a01b0316826001600160a01b031614801561122d575084818151811061122357611223611dd4565b6020026020010151155b1561126657600185828151811061124657611246611dd4565b9115156020928302919091019091015261125f84611aa7565b9350611278565b8061127081611aa7565b9150506111ce565b5050808061128590611aa7565b91505061119b565b50846101000151811015955050505050505b9392505050565b60006112bb6001600160a01b0384168361130e565b905080516000141580156112e05750808060200190518101906112de9190611c18565b155b1561025057604051635274afe760e01b81526001600160a01b03841660048201526024015b60405180910390fd5b606061129f8383600084600080856001600160a01b031684866040516113349190611d7a565b60006040518083038185875af1925050503d8060008114611371576040519150601f19603f3d011682016040523d82523d6000602084013e611376565b606091505b5091509150611386868383611390565b9695505050505050565b6060826113a5576113a0826113ec565b61129f565b81511580156113bc57506001600160a01b0384163b155b156113e557604051639996b31560e01b81526001600160a01b0385166004820152602401611305565b508061129f565b8051156113fc5780518082602001fd5b604051630a12f52160e11b815260040160405180910390fd5b82805482825590600052602060002090810192821561146a579160200282015b8281111561146a57825182546001600160a01b0319166001600160a01b03909116178255602090920191600190910190611435565b5061147692915061147a565b5090565b5b80821115611476576000815560010161147b565b60008083601f8401126114a157600080fd5b5081356001600160401b038111156114b857600080fd5b6020830191508360208285010111156114d057600080fd5b9250929050565b80356001600160a01b03811681146114ee57600080fd5b919050565b600080600080600080600060c0888a03121561150e57600080fd5b87356001600160401b0381111561152457600080fd5b6115308a828b0161148f565b90985096506115439050602089016114d7565b9450604088013593506060880135925061155f608089016114d7565b915060a0880135905092959891949750929550565b60008083601f84011261158657600080fd5b5081356001600160401b0381111561159d57600080fd5b6020830191508360208260051b85010111156114d057600080fd5b60008060008060008060008060c0898b0312156115d457600080fd5b88356001600160401b03808211156115eb57600080fd5b6115f78c838d0161148f565b909a50985088915061160b60208c016114d7565b975060408b0135965061162060608c016114d7565b955060808b0135945060a08b013591508082111561163d57600080fd5b5061164a8b828c01611574565b999c989b5096995094979396929594505050565b6000806020838503121561167157600080fd5b82356001600160401b0381111561168757600080fd5b6116938582860161148f565b90969095509350505050565b600080600080600080600080600060e08a8c0312156116bd57600080fd5b89356001600160401b03808211156116d457600080fd5b6116e08d838e0161148f565b909b5099508991506116f460208d016114d7565b985060408c0135975060608c0135965061171060808d016114d7565b955060a08c0135945060c08c013591508082111561172d57600080fd5b5061173a8c828d01611574565b915080935050809150509295985092959850929598565b60008060008060008060a0878903121561176a57600080fd5b86356001600160401b0381111561178057600080fd5b61178c89828a0161148f565b909750955061179f9050602088016114d7565b9350604087013592506117b4606088016114d7565b9150608087013590509295509295509295565b600080600080606085870312156117dd57600080fd5b84356001600160401b038111156117f357600080fd5b6117ff8782880161148f565b90955093506118129050602086016114d7565b9396929550929360400135925050565b6000806000806000806080878903121561183b57600080fd5b86356001600160401b038082111561185257600080fd5b61185e8a838b0161148f565b909850965086915061187260208a016114d7565b955060408901359450606089013591508082111561188f57600080fd5b5061189c89828a01611574565b979a9699509497509295939492505050565b634e487b7160e01b600052604160045260246000fd5b604051601f8201601f191681016001600160401b03811182821017156118ec576118ec6118ae565b604052919050565b600082601f83011261190557600080fd5b81356001600160401b0381111561191e5761191e6118ae565b611931601f8201601f19166020016118c4565b81815284602083860101111561194657600080fd5b816020850160208301376000918101602001919091529392505050565b6000806040838503121561197657600080fd5b8235915060208301356001600160401b0381111561199357600080fd5b61199f858286016118f4565b9150509250929050565b600080600080604085870312156119bf57600080fd5b84356001600160401b03808211156119d657600080fd5b6119e28883890161148f565b909650945060208701359150808211156119fb57600080fd5b50611a0887828801611574565b95989497509550505050565b60006001600160401b0380841115611a2e57611a2e6118ae565b8360051b6020611a3f8183016118c4565b868152918501918181019036841115611a5757600080fd5b865b84811015611a8b57803586811115611a715760008081fd5b611a7d36828b016118f4565b845250918301918301611a59565b50979650505050505050565b8183823760009101908152919050565b600060018201611ac757634e487b7160e01b600052601160045260246000fd5b5060010190565b6001600160a01b039384168152919092166020820152604081019190915260600190565b60005b83811015611b0d578181015183820152602001611af5565b50506000910152565b60008151808452611b2e816020860160208601611af2565b601f01601f19169290920160200192915050565b6001600160a01b03868116825285166020820152604081018490526060810183905260a060808201819052600090611b7c90830184611b16565b979650505050505050565b60006101008083018b8452602060018060a01b03808d1682870152808c1660408701528a606087015289608087015280891660a08701528760c08701528360e08701528293508654808452610120870194508760005282600020935060005b81811015611c04578454831686529483019460019485019401611be6565b50939e9d5050505050505050505050505050565b600060208284031215611c2a57600080fd5b8151801515811461129f57600080fd5b600181811c90821680611c4e57607f821691505b602082108103611c6e57634e487b7160e01b600052602260045260246000fd5b50919050565b601f82111561025057600081815260208120601f850160051c81016020861015611c9b5750805b601f850160051c820191505b818110156102e657828155600101611ca7565b6001600160401b03831115611cd157611cd16118ae565b611ce583611cdf8354611c3a565b83611c74565b6000601f841160018114611d195760008515611d015750838201355b600019600387901b1c1916600186901b178355611d73565b600083815260209020601f19861690835b82811015611d4a5786850135825560209485019460019092019101611d2a565b5086821015611d675760001960f88860031b161c19848701351681555b505060018560011b0183555b5050505050565b60008251611d8c818460208701611af2565b9190910192915050565b60408152826040820152828460608301376000606084830101526000601f19601f850116820160608382030160208401526113866060820185611b16565b634e487b7160e01b600052603260045260246000fdfea2646970667358221220421f49600440ca5479326c4ded90c851eb91161755c7dda34b0a622751d1960864736f6c63430008140033";

    public static final String FUNC_claimCommitment = "claimCommitment";

    public static final String FUNC_commit = "commit";

    public static final String FUNC_commitWithToken = "commitWithToken";

    public static final String FUNC_COMMITMENTHASH = "commitmentHash";

    public static final String FUNC_RECOVERSIGNER = "recoverSigner";

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
                FUNC_claimCommitment, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(swapId)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> claimCommitment(String swapId, List<byte[]> signatures) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_claimCommitment, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(swapId), 
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.DynamicBytes>(
                        org.web3j.abi.datatypes.DynamicBytes.class,
                        org.web3j.abi.Utils.typeMap(signatures, org.web3j.abi.datatypes.DynamicBytes.class))), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> commit(String swapId, String recipient, BigInteger signaturesThreshold) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_commit, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(swapId), 
                new org.web3j.abi.datatypes.Address(160, recipient), 
                new org.web3j.abi.datatypes.generated.Uint256(signaturesThreshold)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> commit(String swapId, String recipient, BigInteger signaturesThreshold, List<String> signers) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_commit, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(swapId), 
                new org.web3j.abi.datatypes.Address(160, recipient), 
                new org.web3j.abi.datatypes.generated.Uint256(signaturesThreshold), 
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Address>(
                        org.web3j.abi.datatypes.Address.class,
                        org.web3j.abi.Utils.typeMap(signers, org.web3j.abi.datatypes.Address.class))), 
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

    public RemoteFunctionCall<TransactionReceipt> commitWithToken(String swapId, String tokenAddress, BigInteger amount, String recipient, BigInteger signaturesThreshold, List<String> signers) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_commitWithToken, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(swapId), 
                new org.web3j.abi.datatypes.Address(160, tokenAddress), 
                new org.web3j.abi.datatypes.generated.Uint256(amount), 
                new org.web3j.abi.datatypes.Address(160, recipient), 
                new org.web3j.abi.datatypes.generated.Uint256(signaturesThreshold), 
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Address>(
                        org.web3j.abi.datatypes.Address.class,
                        org.web3j.abi.Utils.typeMap(signers, org.web3j.abi.datatypes.Address.class))), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> commitWithToken(String swapId, String tokenAddress, BigInteger tokenId, BigInteger amount, String recipient, BigInteger signaturesThreshold, List<String> signers) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_commitWithToken, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(swapId), 
                new org.web3j.abi.datatypes.Address(160, tokenAddress), 
                new org.web3j.abi.datatypes.generated.Uint256(tokenId), 
                new org.web3j.abi.datatypes.generated.Uint256(amount), 
                new org.web3j.abi.datatypes.Address(160, recipient), 
                new org.web3j.abi.datatypes.generated.Uint256(signaturesThreshold), 
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Address>(
                        org.web3j.abi.datatypes.Address.class,
                        org.web3j.abi.Utils.typeMap(signers, org.web3j.abi.datatypes.Address.class))), 
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

    public RemoteFunctionCall<String> recoverSigner(byte[] messageHash, byte[] signature) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_RECOVERSIGNER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(messageHash), 
                new org.web3j.abi.datatypes.DynamicBytes(signature)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
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
