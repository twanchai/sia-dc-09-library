# ANSI/SIA DC-09 Library

This Java library provides a complete implementation of the ANSI/SIA DC-09 protocol, used for handling alarm messages in security systems.

It is modular and divided into four components:

- **`sia-dc-09-parsing`**: Responsible for message parsing and serialization.
- **`sia-dc-09-commons`**: Defines shared network primitives used by both server and client modules.
- **`sia-dc-09-server`**: Implements server-side communication with alarm devices.
- **`sia-dc-09-client`**: Implements client-side communication from devices to servers.

To host a server, you can start `ch.swissdotnet.siadc09.server.tests.BothTcpUdpServerTestManual` in server package. As for the client, you can use
`ch.swissdotnet.siadc09.client.test.ManualUdpClient` in client package.

## Maintainer

Maintained by [Swissdotnet](https://www.swissdotnet.ch)

## Support

For questions, bug reports, or feature requests, please open a ticket in
the [GitHub issue tracker](https://github.com/Swissdotnet/sia-dc-09-library/issues).

## License

This project is dual-licensed:

- **Non-commercial use** is permitted under the [Polyform Noncommercial License 1.0.0](https://polyformproject.org/licenses/noncommercial/1.0.0/)
- **Commercial use** requires a separate license. Please contact [info@swissdotnet.ch](mailto:info@swissdotnet.ch) for licensing inquiries.

## Integrated Alarm Reception Solutions (IP RCT)

Swissdotnet also offers fully integrated and certified IP alarm reception solutions (IP-RCT) based on this protocol, developed and maintained entirely in Switzerland, including our own proprietary receivers.

Like our transmission platform, our IP alarm reception systems are modular, open, scalable, and compliant with demanding industry standards.

For more information, you can access [our website](https://swissdotnet.ch/en/alarm-transmission/).