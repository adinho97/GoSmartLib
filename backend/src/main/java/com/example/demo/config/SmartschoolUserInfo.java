package com.example.demo.config;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SmartschoolUserInfo {
    @JsonAlias({ "userID", "userid", "userId", "id" })
    private String sub; // user id
    private String name;
    @JsonProperty("given_name")
    @JsonAlias({ "firstname", "voornaam", "firstName" })
    private String givenName;
    @JsonProperty("family_name")
    @JsonAlias({ "surname", "naam", "achternaam", "lastName" })
    private String familyName;
    private String role;
    @JsonAlias({ "Basisrol", "basisrol", "type", "function" })
    private String basisrol; // Smartschool uses this

    public String getSub() {
        return sub;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public String getRole() {
        if (this.basisrol != null) {
            switch (this.basisrol.toLowerCase()) {
                case "leerling":
                    return "leerling";
                case "leerkracht":
                    return "leerkracht";
                // 'bibbeheerder' is a custom app role and won't come from Smartschool.
                // TODO: implement custom logic to assign this role.
                default:
                    return "leerling"; // default to least privileged
            }
        }
        // Fallback: If basisrol is missing but we have a user (name or sub), default to
        // 'leerling'
        // so the frontend receives a valid role and redirects to dashboard.
        if (this.name != null || this.sub != null) {
            return "leerling";
        }
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getBasisrol() {
        return basisrol;
    }

    public void setBasisrol(String basisrol) {
        this.basisrol = basisrol;
    }
}