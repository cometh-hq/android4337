<p align="center">
  <img src="https://github.com/cometh-hq/android4337/blob/3e30328458e6a441c0be632189d97a0896f5816b/cometh-logo.png" alt="Cometh"/>
</p>

# Android4337

Android4337 is an Android SDK for building with [ERC-4337](https://eips.ethereum.org/EIPS/eip-4337).

- **Smart Account**: We offer a high-level API for deploying and managing smart accounts (currently supporting Safe Account).
- **Bundler**: Comprehensive support for all bundler methods as defined
  by [ERC-4337](https://eips.ethereum.org/EIPS/eip-4337#rpc-methods-eth-namespace).
- **Paymaster**: Enables paymaster for gas fee sponsorship.
- **Signers**: Supports both traditional EOA signers and Passkey signers, enabling flexible and secure authentication mechanisms.
- **Modular and Extensible**: Easily create and integrate your own smart account, bundlers, paymasters, and signers.

## Installation

Android4337 SDK is published on Jitpack. To use it, add Jitpack repository to your root gradle file:

```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' } // add this line
    }
}
```

Add the dependency:

```gradle
dependencies {
    implementation "com.github.cometh-hq:android4337:<latest_version>"
}
```

set <last_version> instead of fixed version to get the latest version.

Don't forget to add the internet permission to your AndroidManifest.xml file:

```xml

<uses-permission android:name="android.permission.INTERNET" />
```

## Getting Started

### Overview

```kotlin
// ...

val signer = EOASigner(WalletUtils.loadCredentials("MY_PASSWORD", "MY_PATH"))
val rpcService = HttpService("https://an-infura-or-similar-url.com/123")
val bundlerClient = SimpleBundlerClient(HttpService("https://cometh-or-similar-4337-provider/123"))

val chainId = 11155111 // for example, sepolia
val safeAccount = SafeAccount.createNewAccount(signer, bundlerClient, chainId, rpcService)

safeAccount.sendUserOperation("TO_ADDRESS", value = BigInteger.ZERO, data = "0x".toByteArray())
```

### Smart Account

Allows users to interact with their smart accounts, encapsulating ERC-4337 logic such as deploying the smart account on the first operation,
estimating user operations, and sponsoring gas.

#### Safe Account

In this version of Android4337, we provide support for [Safe Accounts](https://safe.global/).

##### a) Create a SafeAccount for a new address:

```kotlin
fun createNewAccount(
  signer: Signer,
  bundlerClient: BundlerClient,
  chainId: Int,
  web3Service: Service,
  config: SafeConfig = SafeConfig.getDefaultConfig(),
  entryPointAddress: String = EntryPointContract.ENTRY_POINT_ADDRESS_V7,
  paymasterClient: PaymasterClient? = null,
  gasPriceProvider: UserOperationGasPriceProvider = RPCGasEstimator(web3Service),
)
```

- **signer**: If not provided, user operation will be signed with `EcdsaSigner` using the provided credentials. For Passkey, use `PasskeySigner`.
- **config**: If not provided, the default configuration will be used (
  see [Safe Config](https://github.com/cometh-hq/android4337/blob/main/android4337/src/main/java/io/cometh/android4337/safe/SafeConfig.kt)).
- **entryPointAddress**: The address of the entry point contract. By default, is uses the V7 entry point address.
- **paymasterClient**: If specified, it will be used when preparing the user operation to sponsor gas fees.
- **gasPriceProvider**: If not provided, the `RPCGasEstimator` will be used with default parameters (
  see [Gas Price Provider](https://github.com/cometh-hq/android4337/tree/features/add-docs?tab=readme-ov-file#gas-price-provider)).
- **web3jTransactionManager**: If not provided, the `RawTransactionManager` will be used with the provided credentials. `RawTransactionManager` is
  provided by web3j, you can use any other implementation of `TransactionManager` that you want.

##### b) Create a SafeAccount from an existing address:

```kotlin
@WorkerThread
@Throws(IOException::class, RuntimeException::class)
fun fromAddress(
  address: String,
  signer: Signer,
  bundlerClient: BundlerClient,
  chainId: Int,
  web3Service: Service,
  config: SafeConfig = SafeConfig.getDefaultConfig(),
  entryPointAddress: String = EntryPointContract.ENTRY_POINT_ADDRESS_V7,
  paymasterClient: PaymasterClient? = null,
  gasPriceProvider: UserOperationGasPriceProvider = RPCGasEstimator(web3Service),
)
```

Only difference is that you need to provide the address of the Safe Account.

#### Smart Account

Any smart account implementation have to extend `SmartAccount` abstract class and implement the following methods:

```kotlin
abstract fun signOperation(userOperation: UserOperation, entryPointAddress: String): ByteArray
abstract fun getCallData(to: Address, value: BigInteger, data: ByteArray): ByteArray
abstract fun getFactoryAddress(): Address
abstract fun getFactoryData(): ByteArray
abstract fun getDummySignature(): String
```

`SmartAccount` provides the following methods::

- `fun prepareUserOperation(to: Address, value: BigInteger, data: ByteArray): UserOperation` Prepares the user operation, get the initCode if the
  account is not deployed, calls the paymaster if available, and obtains the gas estimation.
- `fun sendUserOperation(to: Address, value: BigInteger, data: ByteArray): String` Prepares the user operation, signs it, sends it to the bundler, and
  returns a user operation hash.
- `fun getNonce(): BigInteger` Returns the current nonce for the smart wallet from the entry point.
- `fun isDeployed(): Boolean` Returns true if the smart account is already deployed.

To be compatible with Android4337, a smart account must provide the following methods (currently, we support Safe Accounts and
provide [the implementation](https://github.com/cometh-hq/android4337/blob/main/android4337/src/main/java/io/cometh/android4337/safe/SafeAccount.kt)):

- `fun signOperation(userOperation: UserOperation, entryPointAddress: String): ByteArray` Signs the user operation with the signer associated with the
  smart account.
- `fun getCallData(to: Address, value: BigInteger, data: ByteArray): ByteArray` Returns the callData to execute the transactions parameters (to,
  value, data and operation).
- `fun getFactoryAddress(): Address` Returns the address of the factory to be used to deploy the wallet.
- `fun getFactoryData(): ByteArray` Returns the call data to be passed to the factory to deploy the wallet.

### Credentials

To control a Smart Account, users need to provide their credentials. Android4337 uses the `Credentials` class
from [web3j](https://github.com/hyperledger/web3j/blob/release/4.8.9-android/crypto/src/main/java/org/web3j/crypto/Credentials.java).

web3j offers a variety of methods to create or get credentials, such as:

```kotlin
val credentials = WalletUtils.loadCredentials("MY_PASSWORD", "MY_PATH")
```

Please refer to the [web3j documentation](https://docs.web3j.io/4.11.0/transactions/credentials/) for more information.

### RPC Service

To interact with the blockchain and call methods on smart contracts, you need a RPC.

```kotlin
val rpcService = HttpService("https://an-infura-or-similar-url.com/123")
```

`HttpService` is a class from web3j. You can use any other implementation of `Service` or create your own.

### Bundler Client

To send, estimate, and get user operations receipts, you need a Bundler.

```kotlin
val bundlerService = HttpService("https://cometh-or-similar-4337-provider/123")
val bundlerClient = SimpleBundlerClient(bundlerService)
```

Available Bundler RPC methods:

- **eth_sendUserOperation**: This method submits a User Operation (UserOp) to the mempool. If the operation is accepted, it returns a userOpHash.

- **eth_estimateUserOperationGas** : Estimates the gas values required for a given User Operation, including PreVerificationGas, VerificationGas, and
  CallGasLimit.

- **eth_getUserOperationByHash**: Retrieves a User Operation and its transaction context based on a given userOpHash.

- **eth_getUserOperationReceipt**: Fetches the receipt of a User Operation based on a given userOpHash. The receipt includes metadata and the final
  status of the UserOp.

You can create your own Bundler Client by implementing the `BundlerClient` interface.

### Paymaster Client

To sponsor gas fees, you need a Paymaster.

```kotlin
val paymasterClient = PaymasterClient("https://cometh-or-similar-4337-provider/123")
```

Available Paymaster RPC methods:

- **pm_sponsorUserOperation**: Submit a UserOperation to the paymaster. If approved for sponsorship, it returns the paymasterAndData along with
  updated gas values
- **pm_supportedEntryPoints**: returns an array of supported EntryPoint addresses.

### Gas Price Provider

To estimate gas prices (`maxFeePerGas` and `maxPriorityFeePerGas`).

We provide an implementation `RPCGasEstimator` but you can create your own by implementing the `UserOperationGasPriceProvider` interface.

```kotlin
interface UserOperationGasPriceProvider {
    fun getGasPrice(): GasPrice
}
```

See our Gas Provider
implementation [RPCGasEstimator](https://github.com/cometh-hq/android4337/blob/main/android4337/src/main/java/io/cometh/android4337/gasprice/RPCGasEstimator.kt)
for more details.

### Signer

To sign user operations, you need a Signer. Android4337 provides two implementations:

- `EOASigner`: Signs user operations using the provided credentials. Used by default.
- `PasskeySigner`: Signs user operations using a Passkey.

#### Passkey Signer

Passkeys provide enhanced security and simplify authentication through quick methods like biometrics. Supported by Apple, Google, and Microsoft, they are widely implemented on iOS and Android. Their adoption improves the user experience by making authentication faster and simpler.

On chain contracts use ERC-1271 and WebAuthn standards for verifying WebAuthn signatures with the secp256r1 curve.


> [!IMPORTANT]  
> To enable passkey support for an Android app, you must associate your app with a website.
> Follow these instructions from the [official Android guide](https://developer.android.com/identity/sign-in/credential-manager#add-support-dal).

> [!IMPORTANT]  
> When initializing a Safe Account with a Passkey signer it will use the Safe WebAuthn Shared Signer to respect 4337 limitation. For more information have a look at [Safe Documentation](https://github.com/safe-global/safe-modules/tree/main/modules/passkey/contracts/4337#safe-webauthn-shared-signer)

##### Safe WebAuthn Shared Signer

There is one notable caveat when using the passkey module with ERC-4337 specifically, which is that ERC-4337 user operations can only deploy exactly one CREATE2 contract whose address matches the sender of the user operation. This means that deploying both the Safe account and its WebAuthn credential owner in a user operation's initCode is not possible.

In order to by pass this limitation you can use the SafeWebAuthnSharedSigner: a singleton that can be used as a Safe owner.

For more Infos : [Safe passkey module](https://github.com/safe-global/safe-modules/blob/main/modules/passkey/contracts/4337/README.md#overview)

To sign user operations using the Passkey, you need to create a `PasskeySigner` instance and launch the passkey creation user flow.

```kotlin
coroutineScope.launch {
    // will launch a passkey creation user flow if passkey not created yet, otherwise will use the existing passkey in Shared Preferences
    // can throw a GetCredentialException (from CredentialsManager) if create_credentials fails
    val passkeySigner = PasskeySigner.withSharedSigner(
        context = context, // Android context
        rpId = "my.rp.id", // must be your package name
        userName = "user_name",
    )
    val passkey = passkeySigner.passkey // returns the passkey if created
}
```

Then, you can use the `PasskeySigner` instance when creating a Safe Account:

```kotlin

val safeAccount = SafeAccount.createNewAccount(
    // ...
    signer = passkeySigner
)

// when sending an user operation, it will prompt the user to sign the operation using the passkey
safeAccount.sendUserOperation("TO_ADDRESS", value = BigInteger.ZERO, data = "0x".toByteArray())
```

This will init a safe with a Passkey Signer using the Safe WebAuthn Shared Signer contract as owner.
When deploying the safe, the Safe WebAuthn Shared Signer will be configured with the x and y of the passkey used.

You can check the sample app for a complete example (see [sample](https://github.com/cometh-hq/android4337/tree/main/example)).

##### Safe WebAuthn Signer

> [!IMPORTANT]  
> This configuration of Passkey signer should not be used for an undeployed safe, or the signer contract should have already been deployed. Due to 4337 limitations, only one CREATE2 contract should be deployed by the init code. Safe and signer cannot be deployed simultaneously.

```kotlin
val signer = EOASigner(WalletUtils.loadCredentials("MY_PASSWORD", "MY_PATH"))
val safeAccount = SafeAccount.createNewAccount(signer, bundlerClient, chainId, rpcService)
val accountAddress = safeAccount.safeAddress

coroutineScope.launch {
    val passkeySigner = PasskeySigner.withSigner(
        context = context,
        rpId = "my.rp.id",
        userName = "user_name",
        web3jService = rpcService, // needed to get signer from the chain
    )
    // This will deploy the Safe Webauthn Contract and add its address as the owner of the safe
    // It can throw a SmartAccountException
    val userOpHash = safeAccount.deployAndEnablePasskeySigner(passkeySigner.passkey.x, passkeySigner.passkey.y)

    // Create a SafeAccount with the new Passkey Signer
    val safePassKeyAccount = SafeAccount.fromAddress(
        address = accountAddress,
        signer = signer,
        bundlerClient = bundlerClient,
        chainId = chainId,
        web3Service = rpcService,
    )
}
```

### Recovery module

#### Enable Recovery Module

Android4337 provides a way to enable a recovery module for a Safe Account. In our implementation, we use [Delay Module](https://github.com/gnosisguild/zodiac-modifier-delay) as recovery module.

Here is the API we provide:

```kotlin
fun enableRecovery(guardianAddress: Address, recoveryModuleConfig: RecoveryModuleConfig = RecoveryModuleConfig()): String
fun getCurrentGuardian(delayAddress: Address): Address?
fun isRecoveryStarted(delayAddress: Address): Boolean
fun cancelRecovery(delayAddress: Address): String
```

- **enableRecovery**: Enables the recovery module for the safe account by passing the guardian address and the recovery module configuration.
- **getCurrentGuardian**: Returns the current guardian address (if any) for the delay module.
- **isRecoveryStarted**: Returns true if the recovery process has started.
- **cancelRecovery**: Cancels the recovery process (if any).

`RecoveryModuleConfig` describes the configuration used for the recovery module, we provides default values:

```kotlin
data class RecoveryModuleConfig(
    val moduleFactoryAddress: String = "0x000000000000aDdB49795b0f9bA5BC298cDda236",
    val delayModuleAddress: String = "0xd54895B1121A2eE3f37b502F507631FA1331BED6",
    val recoveryCooldown: Int = 86400,
    val recoveryExpiration: Int = 604800,
)
```

You can override the default values by providing your own `RecoveryModuleConfig`.

### Connect API

Android4337 provides seamless integration of the Connect API for 4337.
Here are the features we provide:

```kotlin
const apiKey = "your_api_key"
val connectApi = ConnectApi(apiKey)
val result = connectApi.initWallet(chainId, smartAccountAddress, initiatorAddress, publicKeyId, publicKeyX, publicKeyY, deviceData)
//...
val result = connectApi.createWebAuthnSigner(chainId, walletAddress, publicKeyId, publicKeyX, publicKeyY, deviceData, signerAddress, isSharedWebAuthnSigner)
//...
val result = connectApi.getPasskeySignersByWalletAddress(walletAddress)
//...
val result = connectApi.isValidSignature(walletAddress, message, signature, chainId)
```

- **initWallet**: Initialize a smart account.
- **getPasskeySignersByWalletAddress**: Get the list of passkey signers for a given wallet address.
- **createWebAuthnSigner**: Create the webauthn signer for a given wallet address and public key.
- **isValidSignature**: Check if a signature is valid for a given message.


## Dependencies

Android4337 is built on top of [web3j](https://github.com/hyperledger/web3j), an excellent Java (Android compatible) library for working with web3.

At this time, the most recent version of web3j compatible with Android is `4.8.9-android` that we use in our project.

We encourage you to read the web3j [web3j documentation](https://github.com/hyperledger/web3j/blob/main/README.md) for more details on how to use
these components.

## Contributors

The initial project was crafted by the team at Cometh. However, we encourage anyone to help implement new features and to keep this library
up-to-date. Please follow the [contributing guidelines](https://github.com/cometh-hq/android4337/blob/main/CONTRIBUTING.md).

## License

Released under the [Apache License](https://github.com/cometh-hq/android4337/blob/main/LICENSE.txt).



