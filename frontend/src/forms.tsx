type ProfileMetadata = { title: string; description: string | null }

export function MetadataFields({ initial }: { initial?: ProfileMetadata }) {
  return (
    <>
      <label>
        Title
        <input name="title" required minLength={3} maxLength={50} defaultValue={initial?.title} autoFocus />
      </label>
      <label>
        Description
        <textarea name="description" rows={4} maxLength={400} defaultValue={initial?.description ?? ''} />
      </label>
    </>
  )
}
