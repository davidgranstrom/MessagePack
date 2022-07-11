// https://github.com/msgpack/msgpack/blob/master/spec.md

MessagePack {
	classvar <>defaultInitialSize = 2048;
	classvar <>defaultMaxDepth = 100;
	classvar <maxSize = 4294967295.0;

	*encode {arg object, options;
		^MessagePackEncoder(options).encode(object);
	}

	*decode {arg data, options;
		^MessagePackDecoder(data, options);
	}

	*print {arg data, width = 4;
		^data.collect {arg byte;
			var str = byte.asHexString(width);
			str[1] = $x;
			str.toLower;
		};
	}
}

MessagePackEncoder {
	var <>options;
	var initialSize, maxDepth;
	var integerAsFloat;
	var forceFloat32;
	var buffer, pos;

	*new {arg options;
		^super.newCopyArgs(options).init;
	}

	init {
		options = options ? ();
		initialSize = options.defaultInitialSize ? MessagePack.defaultInitialSize;
		maxDepth = options.defaultMaxDepth ? MessagePack.defaultMaxDepth;
		integerAsFloat = options.integerAsFloat ? true;
		forceFloat32 = options.forceFloat32 ? false;
	}

	reset {
		buffer = Array.newClear(initialSize);
		pos = 0;
	}

	checkSize {arg size;
		if (pos + size > buffer.size) {
			buffer = buffer.growClear(initialSize);
		}
	}

	writeU8 {arg value;
		this.checkSize(1);
		buffer[pos] = value;
		pos = pos + 1;
	}

	writeU16 {arg value;
		this.checkSize(2);
		value = value.asInteger;
		buffer[pos] = (value >> 8) & 0xff;
		buffer[pos + 1] = value & 0xff;
		pos = pos + 2;
	}

	writeI32 {arg value;
		this.checkSize(4);
		buffer[pos] = floor(value / 0x1000000) % 0x100;
		buffer[pos + 1] = floor(value / 0x10000) % 0x100;
		buffer[pos + 2] = floor(value / 0x100) % 0x100;
		buffer[pos + 3] = value % 0x100;
		pos = pos + 4;
	}

	writeF32 {arg value;
		this.checkSize(4);
		value = value.as32Bits;
		buffer[pos] = (value >> 24) & 0xff;
		buffer[pos + 1] = (value >> 16) & 0xff;
		buffer[pos + 2] = (value >> 8) & 0xff;
		buffer[pos + 3] = value & 0xff;
		pos = pos + 4;
	}

	encode {arg object;
		this.reset;
		this.prEncode(object);
		buffer = buffer.copyRange(0, pos - 1).asInteger;
		^buffer;
	}

	prEncode {arg object, depth = 1;
		if (depth > maxDepth) {
			"Too many nested levels".error;
		};
		case
		{ object.isKindOf(Nil) } {
			this.encodeNil();
		}
		{ object.isKindOf(Boolean) } {
			this.encodeBoolean(object);
		}
		{ object.isKindOf(Number) } {
			this.encodeNumber(object);
		}
		{ object.isKindOf(String) } {
			this.encodeString(object);
		} {
			this.encodeObject(object, depth);
		}
	}

	encodeNil {
		this.writeU8(0xc0);
	}

	encodeBoolean {arg object;
		this.writeU8(object.if(0xc2, 0xc3));
	}

	encodeNumber {arg object;
		if (object.isInteger and:{integerAsFloat.not} ) {
			if (object >= 0) {
				case
				{ object < 0x80 } {
					// positive fixint
					this.writeU8(object);
				}
				{ object < 0x100 } {
					// uint8
					this.writeU8(0xcc);
					this.writeU8(object);
				}
				{ object < 0x10000 } {
					// uint16
					this.writeU8(0xcd);
					this.writeU16(object);
				}
				{ object <= 0x7fffffff } {
					// int32
					this.writeU8(0xd2);
					this.writeI32(object);
				}
				{
					"No support for uint32/uint64 since Integer is signed 32-bit only (%)".format(object).throw;
				}
			} {
				case
				{ object >= -0x20 } {
					// negative fixint
					this.writeU8(0xe0 | (object + 0x20));
				}
				{ object >= -0x80 } {
					// int8
					this.writeU8(0xd0);
					this.writeU8(object);
				}
				{ object >= -0x8000 } {
					// int16
					this.writeU8(0xd1);
					this.writeU16(object);
				}
				{ object >= -0x80000000 } {
					// int32
					this.writeU8(0xd2);
					this.writeI32(object);
				}
				{
					"No support for int64 since Integer is signed 32-bit only (%)".format(object).throw;
				}
			}
		} {
			if (forceFloat32) {
				this.writeU8(0xca);
				this.writeF32(object);
			} {
				this.writeU8(0xcb);
				this.writeF64(object);
			}
		}
	}

	encodeObject {arg object, depth;
		case
		{ object.isKindOf(Dictionary) } {
			this.encodeDictionary(object, depth);
		}
	}

	encodeDictionary {arg object, depth;
		var size = object.size;
		if (size < 16) {

		}
	}
}

MessagePackDecoder {
	*new {arg data, options;
		^super.new.init(options ? ());
	}
}
