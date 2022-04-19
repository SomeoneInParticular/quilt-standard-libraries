# QSL; Attribute Lookup API (v1)

This QSL provides a common framework for accessing game objects without needing to specify how the association is implemented. Mods can register their own custom attribute types here, or use the attributes provided by the sister module `transfer`; the goal of this API to standardize how these are accessed and managed.

* Attributes are any objects which are expected to be accessed and queried by other mod devs, not including the original creator.
* All attribute querying functions ('Lookups') are stored within `AttributeLookupMap`s, with a central directory being available to register new ones
* Each
* Once an attribute type is registered in the directory, it cannot be overridden by another mod later; only the mod that originally added the attribute should register the attribute to the directory!
* A number of utilities have been provided as well

# Registering a New Attribute Type
