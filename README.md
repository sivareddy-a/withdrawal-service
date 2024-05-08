# Withdrawal Service

**Prerequisites :** 

1. Java 17 - [Installation](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
2. Apache Maven - [Installation](https://maven.apache.org/install.html)

**Running the application :**
- To run the application on windows, please run ./run-application.bat
- To run the application on Linux or Mac, please run ./run-application.sh

Once the application runs successfully, you can find the API documentation [here](http://localhost:8080/swagger-ui/index.html)

**API overview :** 
1. `user-controller` helps setup user accounts and CRUD operations for the same.
2. `transaction-controller` 
    /transactions/withdrawl - makes a transfer from user's account to a wallet. It talks to WithdrawalService for approval
    /transactions/transfer - used for account-to-account transfer

**Data Model**

![Screenshot 2024-05-08 at 12.21.38 AM.png](..%2F..%2F..%2F..%2Fvar%2Ffolders%2F19%2F_sspn4ln0kn9xfp8qgynskrw0000gn%2FT%2FTemporaryItems%2FNSIRD_screencaptureui_bmtMJP%2FScreenshot%202024-05-08%20at%2012.21.38%20AM.png)

**Unit Testing**

Unit tests are available under src/test/java
class : TransactionServiceTest
Tests : 
1. testWithdrawalsSimple
2. testWithdrawalsConcurrent
3. testTransfersSimple
4. testTransfersConcurrent
5. testTransferssAndWithdrawalsConcurrent
        


