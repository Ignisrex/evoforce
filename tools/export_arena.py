"""Export the CRYSTAL_CAVE_ARENA Blender scene to libGDX G3DJ.

Run inside Blender (MCP: exec(compile(open(p).read(), p, 'exec'))) or headless:
    blender -b art/blender/environments/crystal_cave/crystal_cave_arena.blend -P tools/export_arena.py

Coordinate contract: blender (x, y, z) -> game (x, z, -y). Materials are IDs only;
colour is applied in Java from CaveTheme.palette by material name.
"""
import bpy, json, os
from mathutils import Matrix, Color

SCENE = "CRYSTAL_CAVE_ARENA"
BAKED = ("ARENA_ENV", "ARENA_FEATURE")     # world transform baked into vertices
INSTANCED = ("ARENA_PROPS",)               # nodes referencing shared LIB mesh data
LIGHTS = ("ARENA_LIGHTS",)                 # empty nodes carrying a translation
R = Matrix(((1, 0, 0, 0), (0, 0, 1, 0), (0, -1, 0, 0), (0, 0, 0, 1)))  # blender -> game
R_INV = R.inverted()
UV_WORLD = 2.5   # texture repeat in world units (matches the procedural cave floor)

repo = (os.path.dirname(os.path.dirname(os.path.abspath(__file__))) if "__file__" in globals()
        else os.path.abspath(os.path.join(os.path.dirname(bpy.data.filepath), "..", "..", "..", "..")))
OUT = os.path.join(repo, "assets", "models", "environments", "crystal_cave", "arena.g3dj")

scn = bpy.data.scenes[SCENE]
dg = bpy.context.evaluated_depsgraph_get()
meshes, materials, nodes = {}, {}, []


def r4(v):
    return round(float(v), 4)


def to_game(v):
    return [r4(v.x), r4(v.z), r4(-v.y)]


def mat_id(ob, index):
    slots = ob.material_slots
    if slots and index < len(slots) and slots[index].material:
        return slots[index].material.name
    return "MAT_stone_dark"


def export_mesh(key, ob, bake):
    """One G3DJ mesh per key, flat-shaded, one part per material slot. Returns part ids by slot."""
    if key in meshes:
        return meshes[key]["_slots"]
    ev = ob.evaluated_get(dg)
    me = ev.to_mesh()
    me.calc_loop_triangles()
    M = ob.matrix_world if bake else Matrix.Identity(4)
    Mn = M.to_3x3().inverted().transposed()
    verts, parts = [], {}
    for tri in me.loop_triangles:
        n = (Mn @ tri.normal).normalized()
        base = len(verts) // 8
        for vi in tri.vertices:
            co = M @ me.vertices[vi].co
            verts += to_game(co) + to_game(n) + [r4(co.x / UV_WORLD), r4(co.y / UV_WORLD)]   # planar UV, tiles every UV_WORLD units
        parts.setdefault(tri.material_index, []).extend((base, base + 1, base + 2))
    ev.to_mesh_clear()
    if len(verts) // 8 > 65535:
        raise RuntimeError(f"{key}: {len(verts)//8} vertices exceeds the short index limit")
    slots = {}
    g3dj_parts = []
    for mi, idx in sorted(parts.items()):
        pid = f"{key}_m{mi}"
        slots[mi] = pid
        g3dj_parts.append({"id": pid, "type": "TRIANGLES", "indices": idx})
        materials.setdefault(mat_id(ob, mi), None)
    meshes[key] = {"id": key, "attributes": ["POSITION", "NORMAL", "TEXCOORD0"], "vertices": verts,
                   "parts": g3dj_parts, "_slots": slots}
    return slots


def node_parts(ob, slots):
    return [{"meshpartid": pid, "materialid": mat_id(ob, mi)} for mi, pid in slots.items()]


for col in scn.collection.children:
    if col.name in BAKED:
        for ob in col.objects:
            if ob.type != 'MESH':
                continue
            slots = export_mesh(ob.name, ob, bake=True)
            nodes.append({"id": ob.name, "parts": node_parts(ob, slots)})
    elif col.name in INSTANCED:
        for ob in col.objects:
            if ob.type != 'MESH':
                continue
            slots = export_mesh(ob.data.name, ob, bake=False)
            t, q, s = (R @ ob.matrix_world @ R_INV).decompose()
            nodes.append({"id": ob.name, "translation": [r4(t.x), r4(t.y), r4(t.z)],
                          "rotation": [r4(q.x), r4(q.y), r4(q.z), r4(q.w)],
                          "scale": [r4(s.x), r4(s.y), r4(s.z)], "parts": node_parts(ob, slots)})
    elif col.name in LIGHTS:
        for ob in col.objects:
            nodes.append({"id": ob.name, "translation": to_game(ob.matrix_world.translation)})


def srgb(c):
    s = Color(c[:3]).from_scene_linear_to_srgb()
    return [r4(s.r), r4(s.g), r4(s.b)]


def material_entry(name):
    mat = bpy.data.materials.get(name)
    entry = {"id": name, "diffuse": [0.5, 0.5, 0.5], "emissive": [0, 0, 0], "opacity": 1.0}
    bsdf = mat.node_tree.nodes.get("Principled BSDF") if mat and mat.use_nodes else None
    if bsdf:
        entry["diffuse"] = srgb(bsdf.inputs["Base Color"].default_value)
        strength = bsdf.inputs["Emission Strength"].default_value
        entry["emissive"] = srgb([x * strength for x in bsdf.inputs["Emission Color"].default_value[:3]])
        alpha = bsdf.inputs["Alpha"].default_value
        if alpha < 1.0:
            entry["opacity"] = r4(alpha)
    return entry


mat_list = [material_entry(n) for n in sorted(materials)]
for m in meshes.values():
    del m["_slots"]

doc = {"version": [0, 1], "id": "crystal_cave_arena", "meshes": list(meshes.values()),
       "materials": mat_list, "nodes": nodes, "animations": []}
os.makedirs(os.path.dirname(OUT), exist_ok=True)
with open(OUT, "w", encoding="utf-8") as f:
    json.dump(doc, f, separators=(",", ":"))

tris = sum(len(p["indices"]) // 3 for m in meshes.values() for p in m["parts"])
print(f"wrote {OUT}: {len(meshes)} meshes, {tris} tris, {len(nodes)} nodes, {len(mat_list)} materials, "
      f"{os.path.getsize(OUT)//1024} KB")
print("palette (sRGB) for CaveTheme:")
for m in mat_list:
    print(f"  {m['id']:16} diffuse={m['diffuse']} emissive={m['emissive']} opacity={m['opacity']}")
