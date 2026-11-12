package com.livraria.ecommerce.infrastructure.security;

import com.livraria.ecommerce.domain.model.user.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final UUID tenantId;
    private final String name;
    private final String email;
    private final UserRole role;
    private final Set<String> permissions;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(UUID id, UUID tenantId, String name, String email, UserRole role, Set<String> permissions) {
        this.id = id;
        this.tenantId = tenantId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.permissions = permissions != null ? permissions : Collections.emptySet();

        List<GrantedAuthority> auths = new ArrayList<>();
        auths.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
        for (String perm : this.permissions) {
            auths.add(new SimpleGrantedAuthority(perm));
        }
        this.authorities = Collections.unmodifiableList(auths);
    }

    public UserPrincipal(UUID id, UUID tenantId, String name, String email, UserRole role) {
        this(id, tenantId, name, email, role, Collections.emptySet());
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public UserRole getRole() {
        return role;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
