## Third version: generators and tests

`eFCSPay` now includes `EfcsMessageFactory`, which creates timestamped and uniquely numbered `HEAD` envelopes for T101/T102/T112/T201 using the typed request DTOs. `mock` includes service tests for typed T101/T102 responses, T112 HEAD-only response, JSON wire casing, and mismatched transaction codes.

The two projects remain independently buildable. The mock project includes a wire DTO copy in `com.efcs.pay.dto` so it does not depend on the eFCSPay Maven artifact. Keep that DTO copy aligned when protocol fields change.

Run checks:

```sh
(cd eFCSPay && mvn test)
(cd mock && mvn test)
```
