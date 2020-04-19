import test.TestService
import test.TestStruct

class MyService : TestService.Iface {
    override fun test(testStruct: TestStruct?) {
        testStruct?.let {
            println(it.getNum())
        }
    }
}