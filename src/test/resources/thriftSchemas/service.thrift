namespace * test

struct TestStruct {
	1:required i32 num = 0,
}


service TestService {
	void test(1:TestStruct testStruct),
}
