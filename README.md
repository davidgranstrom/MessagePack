# MessagePack

SuperCollider implementation of the [MessagePack][msgpack] binary serialization format.

## Installation

```supercollider
Quarks.install("https://github.com/davidgranstrom/MessagePack");
```

## Usage

**Encode**

```supercollider
var object = (compact: true, schema: 0);
var data = MessagePack.encode(object);
MessagePack.print(data); // [ 82, a7, 63, 6f, 6d, 70, 61, 63, 74, c3, a6, 73, 63, 68, 65, 6d, 61, 00 ]
```

**Decode**
```supercollider
var data = [ 130, 167, 99, 111, 109, 112, 97, 99, 116, 195, 166, 115, 99, 104, 101, 109, 97, 0 ];
var object = MessagePack.decode(data);
object.postln; // ( 'compact': true, 'schema': 0 )
```

## Unit test

```supercollider
TestMessagePack.run;
```

## Status

- Encoder
  - [x] Nil
  - [x] Boolean
  - Number
    - [x] positive/negative fixint
    - [x] uint 8/16
    - [x] int 8/16/32
    - [x] float 32/64
  - String
    - [x] fixstr
    - [x] str 8/16/32
  - Bin
    - [ ] bin 8/16/32
  - Ext
    - [ ] fixext
    - [ ] ext 8/16/32
  - Map
    - [x] fixmap
    - [x] map 16/32
  - Array
    - [x] fixarray
    - [x] array 16/32

- Decoder
  - [x] Nil
  - [x] Boolean
  - Number
    - [x] positive/negative fixint
    - [x] uint 8/16
    - [x] int 8/16/32
    - [x] float 32/64
  - String
    - [x] fixstr
    - [x] str 8/16/32
  - Bin
    - [ ] bin 8/16/32
  - Ext
    - [ ] fixext
    - [ ] ext 8/16/32
  - Map
    - [x] fixmap
    - [x] map 16/32
  - Array
    - [x] fixarray
    - [x] array 16/32

## License

```plain
MessagePack
Copyright © 2022 David Granström

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
```

[msgpack]: https://msgpack.org/index.html
