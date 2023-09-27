pragma solidity ^0.8.13;

abstract contract IToken {

	/* Hold status codes */
	bytes32 public constant _HOLD_STATUS_NON_EXISTENT = "nonExistent";
	bytes32 public constant _HOLD_STATUS_NEW = "new";
	bytes32 public constant _HOLD_STATUS_PERPETUAL = "perpetual";
	bytes32 public constant _HOLD_STATUS_CANCELLED = "cancelled";
	bytes32 public constant _HOLD_STATUS_EXECUTED = "executed";

	/* Hold types */
	bytes32 public constant _HOLD_TYPE_NORMAL = "normal";
	bytes32 public constant _HOLD_TYPE_DESTROY = "destroy";

	/*
	 * Get the available balance of the specified account (net balance minus earmarked/held tokens).
	 * @param {string} account The account id.
	 * @return {uint256} Returns the available balance of the account.
	 */
	function getAvailableBalanceOf(
		string calldata account
	) external view virtual returns (uint256);

	/*
	 * Create a hold on some tokens, earmarked to be transferred from one account to another. Tokens on
	 * hold are not spendable until they are released or the hold is executed. The fromAccount can cancel (release)
	 * the hold after it has expired (i.e. after holdTimeout seconds), and the notary and toAccount can cancel (release) the hold
	 * at any time. It is possible to create a hold without specifying a notaryId using an empty string ("").
	 * @param {string} operationId The id of the operation.
	 * @param {string} fromAccount The account to hold from.
	 * @param {string} toAccount The account to hold to.
	 * @param {string} notaryId The notary id.
	 * @param {string} amount The number of tokens.
	 * @param {string} duration The timeout period (seconds).
	 * @param {string} metaData Any public meta data / instructions accompanying the operation.
	 * @return {bool} Returns true upon success.
	 * @dev If successful, emits CreateHoldExecuted(string operationId, string fromAccount, string toAccount, string notaryId, uint256 amount, string metaData)
	 */
	function createHold(
		string calldata operationId,
		string calldata fromAccount,
		string calldata toAccount,
		string calldata notaryId,
		uint256 amount,
		uint256 duration,
		string calldata metaData
	) external virtual returns (bool);

	/* Event emitted after a hold was successfully created. */
	event CreateHoldExecuted(
		string operationId,
		string fromAccount,
		string toAccount,
		string notaryId,
		uint256 amount,
		string metaData
	);

	/*
	 * Cancel an existing hold. The hold fromAccount can cancel the hold after it has expired (i.e. after duration seconds),
	 * and the notaryId and toAccount can cancel the hold at any time.
	 * @param {string} operationId The id of the operation (hold).
	 * @return {bool} A boolean indicating successful execution of the function.
	 * @dev If successful, emits CancelHoldExecuted(string operationId) and HoldCancelledWithReason(string operationId, string reason).
	 */
	function cancelHold(
		string calldata operationId
	) external virtual returns (bool);

	/* Event emitted after a hold was cancelled. */
	event CancelHoldExecuted(
		string operationId
	);

	/*
	 * Execute an existing hold. The hold notaryId (if specified) and fromAccount can execute the hold.
	 * @param {string} operationId The id of the operation (hold).
	 * @return {bool} A boolean indicating successful execution of the function.
	 * @dev If successful, emits ExecuteHoldExecuted(string operationId).
	 */
	function executeHold(
		string calldata operationId
	) external virtual returns (bool);

	/* Event emitted after a hold was executed. */
	event ExecuteHoldExecuted(
		string operationId
	);

	/*
	 * Make an existing hold perpetual.
	 * @param {string} operationId The id of the operation (hold).
	 * @return {bool} A boolean indicating successful execution of the function.
	 * @dev If successful, emits MakeHoldPerpetualExecuted(string operationId).
	 */
	function makeHoldPerpetual(
		string calldata operationId
	) external virtual returns (bool);

	/* Event emitted after a hold was made perpetual. */
	event MakeHoldPerpetualExecuted(
		string operationId
	);

	/*
	 * Get the hold data.
	 * @param {string} operationId The id of the operation (hold).
	 * @return {string} fromAccount The sender account.
	 * @return {string} toAccount The receiver account.
	 * @return {string} notaryId The notary for the hold.
	 * @return {uint256} amount The hold amount.
	 * @return {uint256} expiryTimestamp The expiry timestamp.
	 * @return {string} metaData The meta data.
	 * @return {bytes32} holdStatus Returns the hold data.
	 * @return {bytes32} holdType The hold type.
	 * @return {bytes32} signer The signer.
	 */
	function getHoldData(
		string calldata operationId
	) external view virtual returns (
		string memory fromAccount,
		string memory toAccount,
		string memory notaryId,
		uint256 amount,
		uint256 expiryTimestamp,
		string memory metaData,
		bytes32 holdStatus,
		bytes32 holdType,
		bytes32 signer
	);

	/*
	 * Adds a hold notary for this contract. A Notary can cancel or execute any hold where this particular notaryId has been
	 * specified. It is recommended to only allow owners/administrators of the contract to perform this action.
	 * @param {string} notaryId The id of the hold notary.
	 * @param {address} holdNotaryAdminAddress The ethereum address that can administer this hold notary.
	 * @return {bool} A boolean indicating successful execution of the function.
	 * @dev If successful, emits AddHoldNotaryExecuted(string notaryId, address holdNotaryAdminAddress).
	 */
	function addHoldNotary(
		string calldata notaryId,
		address holdNotaryAdminAddress
	) external virtual returns (bool);

	/*
	 * Returns whether a notary is available to be selected as a hold notary.
	 * @param {string} notaryId The id of the notary.
	 * @return {bool} A boolean indicating whether this notary exists.
	 */
	function isHoldNotary(
		string calldata notaryId
	) external view virtual returns (bool);

	/*
	 * Create tokens into the specified account.
	 * @param {string} operationId The id of the operation.
	 * @param {string} toAccount The account id.
	 * @param {uint256} amount The number of tokens.
	 * @param {string} metaData Any public meta data / instructions accompanying the operation.
	 * @return {bool} A boolean indicating successful execution of the function.`
	 * @dev If successful, emits CreateExecuted(string operationId, string toAccount, uint256 amount, string metaData)
	 * and BalanceUpdate(string operationId, string fromAccount, string toAccount, uint256 amount,
	 * bytes32 balanceUpdateType, uint256 timestamp, address signer, string metaData);
	 */
	function create(
		string calldata operationId,
		string calldata toAccount,
		uint256 amount,
		string calldata metaData
	) external virtual returns (bool);

	/*
	 * Destroy tokens from the specified account.
	 * @param {string} operationId The id of the operation.
	 * @param {string} fromAccount The account id.
	 * @param {uint256} amount The number of tokens.
	 * @param {string} metaData Any public meta data / instructions accompanying the operation.
	 * @return {bool} A boolean indicating successful execution of the function.
	 * @dev If successful, emits DestroyExecuted(string operationId, string fromAccount, uint256 amount, string metaData)
	 * and BalanceUpdate(string operationId, string fromAccount, string toAccount, uint256 amount,
	 * bytes32 balanceUpdateType, uint256 timestamp, address signer, string metaData);
	 */
	function destroy(
		string calldata operationId,
		string calldata fromAccount,
		uint256 amount,
		string calldata metaData
	) external virtual returns (bool);

	/*
	 * Transfer tokens from one account to another.
	 * @param {string} operationId The id of the operation.
	 * @param {string} fromAccount The account id to transfer from.
	 * @param {string} toAccount The account id to transfer to.
	 * @param {uint256} amount The number of tokens.
	 * @param {string} metaData Any public meta data / instructions accompanying the operation.
	 * @return {bool} Returns a boolean indicating successful execution of the function.
	 * @dev If successful, emits TransferExecuted(string operationId, string fromAccount, string toAccount, uint256 amount, string metaData)
	 * and BalanceUpdate(string operationId, string fromAccount, string toAccount, uint256 amount,
	 * bytes32 balanceUpdateType, uint256 timestamp, address signer, string metaData);
	 */
	function transfer(
		string calldata operationId,
		string calldata fromAccount,
		string calldata toAccount,
		uint256 amount,
		string calldata metaData
	) external virtual returns (bool);
}
