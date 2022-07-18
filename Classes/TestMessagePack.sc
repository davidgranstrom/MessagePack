TestMessagePack : UnitTest {
	test_encodeTypes {
		var data;
		data = MessagePack.encode(nil);
		this.assertEquals(data[0], 0xc0, "nil");
		data = MessagePack.encode(true);
		this.assertEquals(data[0], 0xc3, "true");
		data = MessagePack.encode(false);
		this.assertEquals(data[0], 0xc2, "false");
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
		// fixstr
		data = MessagePack.encode("abc");
		this.assertEquals(data[0], 0xa3, "fixstr");
		this.assertEquals(data[1], $a.ascii, "fixstr");
		this.assertEquals(data[2], $b.ascii, "fixstr");
		this.assertEquals(data[3], $c.ascii, "fixstr");
		// str 8
		data = MessagePack.encode($a.dup(255).join);
		this.assertEquals(data[0], 0xd9, "str 8");
		this.assertEquals(data[1], 0xff, "str 8");
		this.assertEquals(data.size, 0xff + 2, "str 8");
		// str 16
		data = MessagePack.encode($a.dup(0xffff).join);
		this.assertEquals(data[0], 0xda, "str 16");
		this.assertEquals(data[1], 0xff, "str 16");
		this.assertEquals(data[2], 0xff, "str 16");
		this.assertEquals(data.size, 0xffff + 3, "str 8");
		// str 32
		data = MessagePack.encode($a.dup(0x10000).join);
		this.assertEquals(data[0], 0xdb, "str 32");
		this.assertEquals(data[1], 0x0, "str 32");
		this.assertEquals(data[2], 0x1, "str 32");
		this.assertEquals(data[3], 0x0, "str 32");
		this.assertEquals(data[4], 0x0, "str 32");
		this.assertEquals(data.size, 0x10000 + 5, "str 32");
		// unicode test
		data = MessagePack.encode("åäö");
		this.assertEquals(data[0], 0xa6, "utf-8");
		this.assertEquals(data[1], 0xc3, "utf-8");
		this.assertEquals(data[2], 0xa5, "utf-8");
		this.assertEquals(data[3], 0xc3, "utf-8");
		this.assertEquals(data[4], 0xa4, "utf-8");
		this.assertEquals(data[5], 0xc3, "utf-8");
		this.assertEquals(data[6], 0xb6, "utf-8");
		data = MessagePack.encode("🎹");
		this.assertEquals(data[0], 0xa4, "utf-8");
		this.assertEquals(data[1], 0xf0, "utf-8");
		this.assertEquals(data[2], 0x9f, "utf-8");
		this.assertEquals(data[3], 0x8e, "utf-8");
		this.assertEquals(data[4], 0xb9, "utf-8");
	}

	test_encodeMap {
		var map = (compact: true, schema: 0);
		var data = MessagePack.encode(map);
		var expected = "82 a7 63 6f 6d 70 61 63 74 c3 a6 73 63 68 65 6d 61 00".split($ );
		var test = MessagePack.print(data);
		this.assertEquals(data.size, 18);
		expected.do {arg value, i;
			this.assertEquals(value, test[i]);
		};
	}

	test_encodeArray {
		var values = [1, 2, 3];
		var data = MessagePack.encode(values);
		this.assertEquals(data.size, 4);
		values = (16..1) / 16.0;
		data = MessagePack.encode(values);
		this.assertEquals(data.size, 147);
		this.assertEquals(data[0], 0xdc);
		this.assertEquals(data[1], 0x0);
		this.assertEquals(data[2], 0x10);
		values = (65536..1) / 65536.0;
		data = MessagePack.encode(values);
		this.assertEquals(data.size, 589829);
		this.assertEquals(data[0], 0xdd);
	}

	test_decodeTypes {
	    var result = -1;
	    var data = [0xc0];
	    result = MessagePack.decode(data);
	    this.assertEquals(result, nil, "nil");
        data = [0xc2];
	    result = MessagePack.decode(data);
	    this.assertEquals(result, false, "boolean");
        data = [0xc3];
	    result = MessagePack.decode(data);
	    this.assertEquals(result, true, "boolean");
	}

	test_decodeNumber {
	    var n, data, result;
	    n = 1;
	    data = MessagePack.encode(n);
	    result = MessagePack.decode(data);
	    this.assertEquals(result, n, "fixint");
	    n = 255;
	    data = MessagePack.encode(n);
	    result = MessagePack.decode(data);
	    this.assertEquals(result, n, "uint8");
	    n = (2 ** 16 - 1).asInteger;
	    data = MessagePack.encode(n);
	    result = MessagePack.decode(data);
	    this.assertEquals(result, n, "uint16");
	    n = -128;
	    data = MessagePack.encode(n);
	    result = MessagePack.decode(data);
	    this.assertEquals(result, n, "int8");
	    n = -32768;
	    data = MessagePack.encode(n);
	    result = MessagePack.decode(data);
	    this.assertEquals(result, n, "int16");
	    n = -2147483648;
	    data = MessagePack.encode(n);
	    result = MessagePack.decode(data);
	    this.assertEquals(result, n, "int32");
	    n = 2 ** 32 - 1;
	    data = MessagePack.encode(n);
	    result = MessagePack.decode(data);
	    this.assertEquals(result, n, "float32");
	    n = 2 ** 64 - 1;
	    data = MessagePack.encode(n);
	    result = MessagePack.decode(data);
	    this.assertEquals(result, n, "float64");
	}

	test_decodeString {
	    var data, result;
	    data = MessagePack.encode("hello world");
	    result = MessagePack.decode(data);
	    this.assertEquals(result, "hello world", "fixstr");
		data = MessagePack.encode("åäö");
	    result = MessagePack.decode(data);
	    this.assertEquals(result, "åäö", "fixstr");
	}

	test_decodeMap {
		var map = (compact: true, schema: 0);
		var data = MessagePack.encode(map);
		var result = MessagePack.decode(data);
		this.assertEquals(result[\compact], true);
		this.assertEquals(result[\schema], 0);
    }

    test_decodeArray {
		var array = (1..15);
		var data = MessagePack.encode(array);
		var result = MessagePack.decode(data);
		this.assertEquals(result.size, 15);
		array = (1..16);
		data = MessagePack.encode(array);
		result = MessagePack.decode(data);
		this.assertEquals(result.size, 16);
		// array = (1..65536); // works but slow..
		// data = MessagePack.encode(array);
		// result = MessagePack.decode(data);
		// this.assertEquals(result.size, 65536);
    }
}
