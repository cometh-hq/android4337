package nc.startapp.passkey

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.cometh.android4337.SmartAccountException
import io.cometh.android4337.bundler.SimpleBundlerClient
import io.cometh.android4337.paymaster.PaymasterClient
import io.cometh.android4337.safe.SafeAccount
import io.cometh.android4337.utils.hexToByteArray
import io.cometh.android4337.utils.hexToAddress
import io.cometh.android4337.utils.toChecksumHex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.tx.RawTransactionManager
import java.math.BigInteger

class MainViewModel : ViewModel() {

    lateinit var safeAccount: SafeAccount

    private val _owners = MutableStateFlow<List<String>>(emptyList())
    val owners: StateFlow<List<String>> = _owners

    val privateKey = "4bddaeef5fb283e847abf0bd480a771b7695d70f413b248dc56c0bb1bb4a0b86"
    val credentials = Credentials.create(privateKey)

    //val rpcService = HttpService("https://eth-sepolia.g.alchemy.com/v2/hrEktDtKEPaNlqvlXpJCHo0zcxh1Q-Ax")
    val rpcService = HttpService("https://base-sepolia.g.alchemy.com/v2/UEwp8FtpdjcL5oekF6CjMzxe1D3768XU")

    //    val bundlerClient = SimpleBundlerClient(HttpService("https://api.pimlico.io/v2/11155111/rpc?apikey=b12e3d58-61b8-4e2b-9c7c-f9c618c0e113"))
    val bundlerClient = SimpleBundlerClient(HttpService("https://base-sepolia.bundler.cometh.io?apikey=Y3dZHg2cc2qOT9ukzvxZZ7jEloTqx5rx"))
//    val bundlerClient = SimpleBundlerClient(HttpService("http://192.168.1.106:3001"))

    init {
        //val credentials = WalletUtils.loadCredentials("MY_PASSWORD", "MY_PATH")
        //val credentials = WalletUtils.loadCre
        //address=0x2f920a66C2f9760f6fE5F49b289322Ddf60f9103
        val address = credentials.address
        Log.i("MainViewModel", "address: $address")
//        safeAccount = SafeAccount.fromAddress(
//            "0x4bF81EEF3911db0615297836a8fF351f5Fe08c68",
//            credentials,
//            bundlerClient,
//            11155111,
//            rpcService,
//        )
    }

    // get owners
    fun fetchOwners() {
        viewModelScope.launch {
            _owners.value = withContext(Dispatchers.IO) {
                val result = safeAccount.getOwners()?.map { it.toChecksumHex() } ?: emptyList()
                return@withContext result
            }
        }
    }

    fun sendUserOperation() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {

                try {
                    val address = SafeAccount.predictAddress(
                        credentials.address,
                        RawTransactionManager(Web3j.build(rpcService), credentials),
                    )
                    Log.i("MainViewModel", "address: $address")
                    safeAccount = SafeAccount.createNewAccount(
                        credentials,
                        bundlerClient,
                        11155111,
                        rpcService,
                    )
                    val hash = safeAccount.sendUserOperation(
                        "0xf1dc86F621F85409bBeA2f14e7E2971E73C28226".hexToAddress(),
                        BigInteger.ZERO,
                        "0x".hexToByteArray(),
                    )
                    Log.i("MainViewModel", "sendUserOperation hash: $hash")
                } catch (e: SmartAccountException) {
                    Log.e("MainViewModel", "sendUserOperation error: ${e.message}")
                }
            }
        }
    }

    fun sendUserOpWithPaymaster() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val paymasterClient = PaymasterClient(
                        paymasterUrl = "https://paymaster.cometh.io/84532/?apikey=Y3dZHg2cc2qOT9ukzvxZZ7jEloTqx5rx"
//                                paymasterUrl = "http://192.168.1.106:3001"
                    )
                    safeAccount = SafeAccount.createNewAccount(
                        credentials,
                        bundlerClient,
                        84532,
                        rpcService,
                        paymasterClient = paymasterClient,
                    )
                    val hash = safeAccount.sendUserOperation(
                        "0xf1dc86F621F85409bBeA2f14e7E2971E73C28226".hexToAddress(),
                        BigInteger.ZERO,
                        "0x".hexToByteArray(),
                    )
                    Log.i("MainViewModel", "sendUserOperation hash: $hash")
                } catch (e: SmartAccountException) {
                    Log.e("MainViewModel", "sendUserOperation error: ${e.message}")
                }
            }
        }
    }


}