package org.epos.dbconnector;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Objects;

@Entity
@Table(name = "configurations")
@NamedQueries({
        @NamedQuery(name = "configurations.findAll", query = "SELECT c FROM Configurations c"),
        @NamedQuery(name = "configurations.findById", query = "SELECT c FROM Configurations c where c.id = :ID")
})
public class Configurations {
    private String id;
    private String configuration;

	@Id
    @Column(name = "id", nullable = false, length = 1024)
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Basic
    @Column(name = "configuration", length = 1024)
    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

	@Override
	public int hashCode() {
		return Objects.hash(configuration, id);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Configurations other = (Configurations) obj;
		return Objects.equals(configuration, other.configuration) && Objects.equals(id, other.id);
	}

	@Override
	public String toString() {
		return "Configurations [id=" + id + ", configuration=" + configuration + "]";
	}


}
