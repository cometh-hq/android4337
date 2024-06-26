package io.cometh.android4337.web3j

import org.junit.Assert
import org.junit.Test

class Create2Test {
    @Test
    fun testCreate2Case1() {
        Create2.getCreate2Address(
            sender = "0xfe58a39Fd369Ba46EAa55812b8204c54A7beCc0F",
            salt = "0x0d151319d893ea212488efd8bb1a22937f590e9a643b71580d52d9f73e4650e1",
            initCodeHash = "0x44436c1b46a02af854be0490f564348d0d496483b3f6f7a9acc797d34c2cecef"
        ).let {
            Assert.assertEquals("0x5f1A74731d91Ce5eC1fdB95faFEc5E012536AEC5", it)
        }
    }

    @Test
    fun testCreate2Case2() {
        Create2.getCreate2Address(
            sender = "0x7783047E5549d1ff83e618D1183394A1c2094e0F",
            salt = "0x30e301625465788bcf0e9bd6b3020028903c8295ea08d341b6fd75cb47de01c1",
            initCodeHash = "0x42e4ee97c4339dee95e31e0a8748f14696870330c28e943facaf4d14f7a6efc3"
        ).let {
            Assert.assertEquals("0x4af51DA95CcD0832f3B7388Fe80C5D2A73e1DB86", it)
        }
    }

    @Test
    fun testCreate2Case3() {
        Create2.getCreate2Address(
            sender = "0x9414c20c4a06ed631ec3891230f921D86E8Fff44",
            salt = "0xc94e7fff7da6b9eb3ccaafa4494aa381836df54991f402cbf28dc46f509619b8",
            initCodeHash = "0x34b0393bffce3aa2914c3a58ebc008b7b6ee0d0087adaabdb4392b0a64920350"
        ).let {
            Assert.assertEquals("0x30E92CFF7A5415F7FCe87DA17913EA7095aC38d9", it)
        }
    }

    @Test
    fun testCreate2Case4() {
        Create2.getCreate2Address(
            sender = "0x9414c20c4a06ed631ec3891230f921D86E8Fff44",
            salt = "0x0739aec466642fc9e3478883be0031ca61b02b4359f1797b86dfbab91c49abec",
            initCodeHash = "0x00f024d57f82d7f2356d49090c4a235eb555e75b38458279c8eb5cf0028e15e3"
        ).let {
            Assert.assertEquals("0x5a071507d12Db92A5e3C5d56A637aFCED77A0Bb8", it)
        }
    }

    @Test
    fun testCreate2Case5() {
        Create2.getCreate2Address(
            sender = "0x7783047E5549d1ff83e618D1183394A1c2094e0F",
            salt = "0x4eae35508ebae1329067689eb995775659a52bbfdc841bfae1e9d9341420b1b6",
            initCodeHash = "0x060fded080de1265fd0e591163eae7ca0de0b3f2b1c1721cacb90231524e2c3d"
        ).let {
            Assert.assertEquals("0x711904ac42CBF8db7e85FB9F3cC9F40aAa1b27AE", it)
        }
    }

    @Test
    fun testCreate2Case6() {
        Create2.getCreate2Address(
            sender = "0x4e1DCf7AD4e460CfD30791CCC4F9c8a4f820ec67",
            salt = "0xdd4357ab477d47899a4976c44762fb59931767bbf890f6be804e42a975c49cb8",
            initCodeHash = "0xe298282cefe913ab5d282047161268a8222e4bd4ed106300c547894bbefd31ee"
        ).let {
            Assert.assertEquals("0x2142Dd0E494701E2e2D4660C63569F510e3A7DF8", it)
        }
    }
}