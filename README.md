### How to build and run

1. Build app:
    `mvn package`
2. Run app (app runs on port 8080):
   ` java -jar .\target\accounts-transfer-service-1.0-SNAPSHOT-jar-with-dependencies.jar`

### Technical notes:

Given it was hinted that spring is not a preferred option, I decided to give it a go with javalin + google guice (for dependency injection). I’m quite happy with how easy and intuitive it was to create a javalin app and writing integration test, so definitely I’ll consider using it in future.
As suggested, for simplicity, all data storage has been implemented in memory (mainly using Maps).
the implementation also assumes that there will be only a single running instance of the app (obvious from previous point anyway).


### Implementation notes:

BigDecimal has been chosen as the type to deal with amounts to make sure precision is not an issue.
When a withdrawal from API happens, the withdrawn amount is immediately deducted from the sender balance as soon as API call is made, this decision has been made to protect against slow API calls causing a potential overdraft state in sender account when suddenly the **INPROGRESS** transactions are acknowledged as **COMPLETED**. 
If transaction finally fails, then money is added back to sender’s balance.
Monitoring of **INPROGRESS** transactions is done in a separate pool of threads to avoid locking main app server threads.

### Assumptions made:

Even it was not in the requirements, an endpoint to create a new account with a given initial balance has been provided for the sake of end to end testing using APIs (build front to back scenarios where an account is created with initial balance and then transfers are made from and to it).
Two different POST endpoints have been provided, one to initiate internal transfer between accounts and another one to start transfers to an external address.
A GET endpoint has been provided to list the state of all transactions made to external addresses (internal tranfers have been excluded from this endpoint, assuming they will be immediately performed and no need to track them)