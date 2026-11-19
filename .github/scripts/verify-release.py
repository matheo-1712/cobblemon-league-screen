"""Validate the release jar before publishing it anywhere."""

import json
import sys
from zipfile import ZipFile


def verify(path, version):
    with ZipFile(path) as jar:
        names = jar.namelist()
        if len(names) != len(set(names)):
            raise ValueError("Duplicate entries in the release jar")

        metadata = json.loads(jar.read("fabric.mod.json"))
        if metadata["id"] != "cobblemon-league-screen" or metadata["version"] != version:
            raise ValueError("Release jar ID/version does not match the requested release")
        for side in ("main", "client"):
            for entry in metadata["entrypoints"][side]:
                name = entry["value"] if isinstance(entry, dict) else entry
                jar.getinfo(name.replace(".", "/") + ".class")

        jar.getinfo("assets/cobblemon-league-screen/lang/en_us.json")
        jar.getinfo("assets/cobblemon-league-screen/lang/fr_fr.json")
        # The Couronne is read from the jar itself: without it, every wall loses its fallback theme.
        jar.getinfo("data/cobblemon-league-screen/leaguescreen/themes/couronne.json")
        jar.getinfo("data/cobblemon-league-screen/loot_table/blocks/terminal.json")

        # Cobblemon Trainers is optional and compile-only: a copy of its classes in this jar would
        # clash with the real mod.
        if any(name.startswith("matheo1712/cobbletrainers/") for name in names):
            raise ValueError("Cobblemon Trainers classes bundled in the League Screen jar")
    print(f"Verified release {version}: {path}")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        raise SystemExit("Usage: verify-release.py JAR VERSION")
    verify(*sys.argv[1:])
