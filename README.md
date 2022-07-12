# MessagePack

SuperCollider implementation of the [MessagePack][msgpack] binary serialization format.

## TODO

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
  - Map
    - [x] fixmap
    - [x] map 16/32
  - Array
    - [ ] fixarray
    - [ ] array 16/32

- Decoder
  - [ ] Nil
  - [ ] Boolean
  - Number
    - [ ] positive/negative fixint
    - [ ] uint 8/16
    - [ ] int 8/16/32
    - [ ] float 32/64
  - String
    - [ ] fixstr
    - [ ] str 8/16/32
  - Map
    - [ ] fixmap
    - [ ] map 16/32
  - Array
    - [ ] fi array
    - [ ] array 16/32

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
