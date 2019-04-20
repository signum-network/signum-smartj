# BlockTalk: easy to use smart contracts for Burstcoin
[![GPLv3](https://img.shields.io/badge/license-GPLv3-blue.svg)](LICENSE)

[Burstcoin](https://www.burst-coin.org/) was the world's first HDD-mined
cryptocurrency using an energy efficient
and fair Proof-of-Capacity (PoC) consensus algorithm.

It was also the first to implement a turing-complete [smart contract](https://www.burst-coin.org/smart-contracts)
system in the form of *Automated Transactions* (AT), as specified by [CIYAM](http://ciyam.org/at/).
However, before BlockTalk, the creation and deployment of smart contracts required writing
(assembler-like) bytecode and testing on-chain, making the development of contracts cumbersome.

This project allows the user to write, debug, and deploy Burst smart contracts relying only in Java.
You can use a simple text editor or your preferred IDE.
BlockTalk consists of the following key components:
 - **[Contract.java](src/main/java/bt/Contract.java)**: a Java abstract class defining the basic API available for contracts
 - **Emulator**: an emulated block-chain and respective UI
 - **Compiler**: a system to convert Java bytecode into Burst AT bytecode that can run on the Burst block-chain 

[![Simple hello world contract](http://img.youtube.com/vi/z06nThjLTQ0/0.jpg)](https://www.youtube.com/watch?v=z06nThjLTQ0 "BlockTalk hello world")

This project is in pre-alpha. Please carefully inspect your compiled AT contracts and
test it exhaustively on the [testnet](https://burstwiki.org/wiki/Testnet) before production.


## Sample Contracts
Take a look on the [samples source folder](src/main/java/bt/sample/).

## License

This code is licensed under [GPLv3](LICENSE).

## Author

jjos
