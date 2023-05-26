package misc

type createOption struct {
	NumPartition      int
	ReplicationFactor int
}

type CreateOption interface {
	apply(*createOption)
}

type createOptionSetter struct {
	set func(*createOption)
}

func (s *createOptionSetter) apply(opt *createOption) {
	s.set(opt)
}

func WithNumPartition(numPartition int) *createOptionSetter {
	return &createOptionSetter{set: func(opt *createOption) { opt.NumPartition = numPartition }}
}

func WithReplicationFactor(replicationFactor int) *createOptionSetter {
	return &createOptionSetter{set: func(opt *createOption) { opt.ReplicationFactor = replicationFactor }}
}
