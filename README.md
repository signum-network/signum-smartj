# Signum SmartJ: easy to use smart contracts for Signum
[![GPLv3](https://img.shields.io/badge/license-GPLv3-blue.svg)](LICENSE)
[![](https://jitpack.io/v/jjos2372/blocktalk.svg)](https://jitpack.io/#jjos2372/blocktalk)

[Signum](https://signum.network/) (previously know as Burstcoin) was the world's first HDD-mined
cryptocurrency using an energy efficient
and fair Proof-of-Capacity (PoC) consensus algorithm.

It was also the first to implement a turing-complete [smart contract](https://signum.network/smartcontracts.html)
system in the form of *Automated Transactions* (AT), as specified by [CIYAM](http://ciyam.org/at/).
However, before SmartJ, the creation and deployment of smart contracts required writing
(assembler-like) bytecode and testing on-chain, making the development of contracts cumbersome.

This project allows the user to write, debug, and deploy Signum smart contracts relying only on Java.
You can use a simple text editor or your preferred IDE.
SmartJ consists of the following key components:
 - **[Contract.java](src/main/java/bt/Contract.java)**: a Java abstract class defining the basic API available for contracts
 - **Emulator**: an emulated blockchain and respective UI
 - **Compiler**: a system to convert Java bytecode into Signum AT bytecode that can run on the Signum blockchain 

[![Simple hello world contract](http://img.youtube.com/vi/XcN5WxqjjGw/0.jpg)](https://www.youtube.com/watch?v=XcN5WxqjjGw "Signum SmartJ sample application")

As any open source project, this is experimental technology.
Please carefully inspect your compiled AT contracts and
test it exhaustively on the [testnet](https://github.com/burst-apps-team/burstcoin#testnet) before production.


## Sample Contracts
Take a look on the [samples source folder](src/main/java/bt/sample/).

## Using (write your own contract)

### Sample application
The easiest way to start with SmartJ is to clone this project and import as an **existing gradle project** using
your preferred IDE.
Check the [samples source folder](src/main/java/bt/sample/) and modify existing contracts or create new ones. 

### Add Signum SmartJ to your gradle project
Add the following to your gradle.build file:
```
repositories {
	maven { url 'https://jitpack.io' }
}
dependencies {
	implementation 'com.github.signum-network:signum-smartj:-SNAPSHOT'
}
```

### Add Signum SmartJ to your maven project
Add the repository to your configuration:
```
<repositories>
	<repository>
	    <id>jitpack.io</id>
	    <url>https://jitpack.io</url>
	</repository>
</repositories>
<dependency>
	<groupId>com.github.jjos2372</groupId>
	<artifactId>blocktalk</artifactId>
	<version>-SNAPSHOT</version>
</dependency>
```

## License

This code is licensed under [GPLv3](LICENSE).

## Author

jjos

Donation address: S-JJQS-MMA4-GHB4-4ZNZU
