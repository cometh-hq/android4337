<p align="center">
  <img src="https://github.com/cometh-hq/android4337/blob/3e30328458e6a441c0be632189d97a0896f5816b/cometh-logo.png" alt="Cometh"/>
</p>

# Android4337

Android4337 is an Android SDK for building with [ERC-4337](https://eips.ethereum.org/EIPS/eip-4337).

- **Smart Account**: We offer a high-level API for deploying and managing smart accounts (currently supporting Safe Account).
- **Bundler**: Comprehensive support for all bundler methods as defined
  by [ERC-4337](https://eips.ethereum.org/EIPS/eip-4337#rpc-methods-eth-namespace).
- **Paymaster**: Enables paymaster for gas fee sponsorship.
- **Modular and Extensible**: Easily create and integrate your own smart account, bundlers, paymasters, and signers.
- **Webauthn and PassKey**: We provide a way to sign user operations using Webauthn and PassKey.

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
    implementation "com.github.cometh-hq:android4337:0.1.0"
}
```

Don't forget to add the internet permission to your AndroidManifest.xml file:

```xml

<uses-permission android:name="android.permission.INTERNET" />
```

## Getting Started

### Overview

```kotlin
import com.cometh.android4337.Android4337

// ...

val credentials = WalletUtils.loadCredentials("MY_PASSWORD", "MY_PATH")
val rpcService = HttpService("https://an-infura-or-similar-url.com/123")

val bundlerService = HttpService("https://cometh-or-similar-4337-provider/123")
val bundlerClient = SimpleBundlerClient(bundlerService)

val chainId = 11155111 // for example, sepolia
val safeAccount = SafeAccount.createNewAccount(credentials, bundlerClient, chainId, rpcService)

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
    credentials: Credentials,
    bundlerClient: BundlerClient,
    chainId: Int,
    web3Service: Service,
    config: SafeConfig = SafeConfig.createDefaultConfig(),
    entryPointAddress: String = EntryPointContract.ENTRY_POINT_ADDRESS_V7,
    signer: Signer = EcdsaSigner(credentials),
    paymasterClient: PaymasterClient? = null,
    gasPriceProvider: UserOperationGasPriceProvider = RPCGasEstimator(web3Service),
    web3jTransactionManager: TransactionManager = RawTransactionManager(Web3j.build(web3Service), credentials)
)
```

- **config**: If not provided, the default configuration will be used (
  see [Safe Config](https://github.com/cometh-hq/android4337/blob/main/android4337/src/main/java/io/cometh/android4337/safe/SafeConfig.kt)).
- **entryPointAddress**: The address of the entry point contract. By default, is uses the V7 entry point address.
- **signer**: If not provided, user operation will be signed with `EcdsaSigner` using the provided credentials. For PassKey, use `PassKeySigner`.
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
    credentials: Credentials,
    bundlerClient: BundlerClient,
    chainId: Int,
    web3Service: Service,
    config: SafeConfig = SafeConfig.createDefaultConfig(),
    entryPointAddress: String = EntryPointContract.ENTRY_POINT_ADDRESS_V7,
    signer: Signer = EcdsaSigner(credentials),
    paymasterClient: PaymasterClient? = null,
    gasPriceProvider: UserOperationGasPriceProvider = RPCGasEstimator(web3Service),
    web3jTransactionManager: TransactionManager = RawTransactionManager(Web3j.build(web3Service), credentials)
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
- `fun getDummySignature(): String` Returns a dummy signature to be used in user operation gas estimation.

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

- `EcdsaSigner`: Signs user operations using the provided credentials. Used by default.
- `PassKeySigner`: Signs user operations using a PassKey.

#### PassKey Signer

To sign user operations using the PassKey, you need to create a `PassKeySigner` instance and launch the passkey creation user flow.

```kotlin
val passKeySigner = PassKeySigner(
    rpId = "my.rp.id",
    context = context, // Android context
)

// must be done before creating SafeAccount, will launch a passkey creation user flow
// can throw a GetCredentialException (from CredentialsManager) if create_credentials fails
coroutineScope.launch {
    passKeySigner.createPasskey(userName = "user_name") // or passKeySigner.importPasskey(x, y)
}

// ...

val safeAccount = SafeAccount.createNewAccount(
    // ...
    signer = passKeySigner
)

// when sending an user operation, it will prompt the user to sign the operation using the passkey
safeAccount.sendUserOperation("TO_ADDRESS", value = BigInteger.ZERO, data = "0x".toByteArray())
```

You can check the sample app for a complete example (see [sample]()).

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



