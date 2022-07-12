TestMessagePack : UnitTest {
	setUp {
		// this will be called before each test
	}

	tearDown {
		// this will be called after each test
	}

	test_encode {
		var data;
		data = MessagePack.encode(nil);
		this.assertEquals(data[0], 0xc0, "nil");
		data = MessagePack.encode(true);
		this.assertEquals(data[0], 0xc2, "true");
		data = MessagePack.encode(false);
		this.assertEquals(data[0], 0xc3, "false");
		// fixint
		data = MessagePack.encode(0, (integerAsFloat: false));
		this.assertEquals(data[0], 0x0, "fixint");
		data = MessagePack.encode(127, (integerAsFloat: false));
		this.assertEquals(data[0], 0x80 - 1, "fixint max");
		data = MessagePack.encode(-32, (integerAsFloat: false));
		this.assertEquals(data[0], 0xe0, "fixint min");
		// uint8
		data = MessagePack.encode(255, (integerAsFloat: false));
		this.assertEquals(data[0], 0xcc, "uint8");
		this.assertEquals(data[1], 0xff, "uint8");
		// uint16
		data = MessagePack.encode(65535, (integerAsFloat: false));
		this.assertEquals(data[0], 0xcd, "uint16");
		this.assertEquals(data[1], 0xff, "uint16");
		this.assertEquals(data[2], 0xff, "uint16");
		// ~ no support for uint32 or uint64
		// int8
		data = MessagePack.encode(-128, (integerAsFloat: false));
		this.assertEquals(data[0], 0xd0, "int8");
		this.assertEquals(data[1], -128, "int8");
		// int16
		data = MessagePack.encode(-0x7fff, (integerAsFloat: false));
		this.assertEquals(data[0], 0xd1, "int16");
		this.assertEquals(data[1], 0x80, "int16");
		this.assertEquals(data[2], 0x01, "int16");
		// int32
		data = MessagePack.encode(0x7fffffff, (integerAsFloat: false));
		this.assertEquals(data[0], 0xd2, "int32 max");
		this.assertEquals(data[1], 0x7f, "int32 max");
		this.assertEquals(data[2], 0xff, "int32 max");
		this.assertEquals(data[3], 0xff, "int32 max");
		this.assertEquals(data[4], 0xff, "int32 max");
		// int32
		data = MessagePack.encode(-0x80000000, (integerAsFloat: false));
		this.assertEquals(data[0], 0xd2, "int32 min");
		this.assertEquals(data[1], 0x80, "int32 min");
		this.assertEquals(data[2], 0x0, "int32 min");
		this.assertEquals(data[3], 0x0, "int32 min");
		this.assertEquals(data[4], 0x0, "int32 min");
		// f32
		data = MessagePack.encode(2 ** 32, (forceFloat32: true));
		this.assertEquals(data[0], 0xca, "f32");
		this.assertEquals(data[1], 0x4f, "f32");
		this.assertEquals(data[2], 0x80, "f32");
		this.assertEquals(data[3], 0x0,  "f32");
		this.assertEquals(data[4], 0x0,  "f32");
		// f64
		data = MessagePack.encode(12.375);
		this.assertEquals(data[0], 0xcb, "f64");
		this.assertEquals(data[1], 0x40, "f64");
		this.assertEquals(data[2], 0x28, "f64");
		this.assertEquals(data[3], 0xc0, "f64");
		this.assertEquals(data[4], 0x0,  "f64");
		this.assertEquals(data[5], 0x0,  "f64");
		this.assertEquals(data[6], 0x0,  "f64");
		this.assertEquals(data[7], 0x0,  "f64");
		this.assertEquals(data[8], 0x0,  "f64");
		data = MessagePack.encode(-12.375);
		this.assertEquals(data[0], 0xcb, "f64");
		this.assertEquals(data[1], 0xc0, "f64");
		this.assertEquals(data[2], 0x28, "f64");
		this.assertEquals(data[3], 0xc0, "f64");
		this.assertEquals(data[4], 0x0,  "f64");
		this.assertEquals(data[5], 0x0,  "f64");
		this.assertEquals(data[6], 0x0,  "f64");
		this.assertEquals(data[7], 0x0,  "f64");
		this.assertEquals(data[8], 0x0,  "f64");
	}
}
